package idawi.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import idawi.ComponentDescriptor;
import idawi.Message;
import toools.thread.Threads;

public class TCPDriver extends IPDriver {
	// public static final int DEFAULT_PORT = IPDriver.DEFAULT_PORT;

	private static class Entry {
		final Socket socket;
		final OutputStream os;

		Entry(Socket s) throws IOException {
			this.socket = s;
			this.os = s.getOutputStream();
		}
	}

	private final Map<ComponentDescriptor, Entry> peer_socket = new HashMap<>();
	private ServerSocket ss;

	@Override
	public String getProtocolName() {
		return "TCP";
	}

	@Override
	public void injectLocalInfoTo(ComponentDescriptor c) {
		super.injectLocalInfoTo(c);
		c.tcpPort = getPort();
	}

	@Override
	public boolean canContact(ComponentDescriptor c) {
		return super.canContact(c) && c.tcpPort != null;
	}

	@Override
	protected void startServer() {
		while (true) {
			try {
				this.ss = new ServerSocket(getPort());
				markReady();

				while (true) {
					Socket incomingSocket = ss.accept();
					newSocket(incomingSocket);
				}
			} catch (IOException e) {
				e.printStackTrace();
				stopServer();
				Threads.sleepMs(1000);
			}
		}
	}

	private void newSocket(Socket socket) throws IOException {
		InputStream is = socket.getInputStream();

		new Thread(() -> {
			try {
				while (true) {
					Message msg = (Message) serializer.read(is);

					if (msg.route.size() > 0) {
						ComponentDescriptor relay = msg.route.last().component;

						synchronized (peer_socket) {
							Entry e = peer_socket.get(relay);

							if (e == null || e.socket != socket) {
								peer_socket.put(relay, new Entry(socket));
							}
						}
					}

					processIncomingMessage(msg);
				}
			} catch (IOException e) {
				// e.printStackTrace();
				errorOn(socket);
			}
		}).start();
	}

	private void errorOn(Socket s) {
		// new Exception().printStackTrace();
		synchronized (peer_socket) {
			Iterator<java.util.Map.Entry<ComponentDescriptor, Entry>> i = peer_socket.entrySet().iterator();

			while (i.hasNext()) {
				java.util.Map.Entry<ComponentDescriptor, Entry> e = i.next();

				if (e.getValue().socket == s) {
					i.remove();
					ComponentDescriptor peer = e.getKey();
					listeners.forEach(l -> l.peerLeft(peer, this));
				}
			}
		}

		try {
			s.close();
		} catch (IOException e1) {
		}
	}

	@Override
	public void send(Message msg, Collection<ComponentDescriptor> neighbors) {
		for (ComponentDescriptor n : neighbors) {
			Entry entry = null;

			synchronized (peer_socket) {
				entry = peer_socket.get(n);

				// there is no connection to this peer yet
				// try to establish one
				if (entry == null) {
					try {
						Socket socket = createSocket(n);

						if (socket != null) {
							peer_socket.put(n, entry = new Entry(socket));
							newSocket(socket);
						}
					} catch (IOException e) {
						return;
					}
				}
			}

			// if a connection could be obtained
			if (entry != null) {
				try {
					serializer.write(msg, entry.os);
				} catch (IOException e) {
					errorOn(entry.socket);
				}
			}
		}
	}

	private Socket createSocket(ComponentDescriptor to) {
		InetAddress ip = to.inetAddresses.get(0);

		try {
			// new Exception().printStackTrace();
			System.out.println("trying to connect to " + ip + " on port " + to.tcpPort);
			Socket s = new Socket(ip, to.tcpPort);
			return s;
		} catch (IOException e) {
			/*
			 * int localPort = NetUtilities.findAvailablePort(1000);
			 * Cout.debug("creating SSH tunnel to " + ip + ":" + port + " using local port "
			 * + localPort);
			 * 
			 * SSHUtils.createSSHTunnelTo(to.sshParameters, ip, port, localPort);
			 * 
			 * try { return new Socket("localhost", localPort); } catch (IOException e1) {
			 * return null; }
			 */
			return null;
		}
	}

	@Override
	public void stopServer() {
		try {
			ss.close();
			ss = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected boolean isRunning() {
		return ss != null;
	}

	@Override
	public Collection<ComponentDescriptor> neighbors() {
		return peer_socket.keySet();
	}
}
