package idawi.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import idawi.Component;
import idawi.knowledge_base.DigitalTwinService;
import idawi.messaging.Message;
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

	private final Map<Component, Entry> neighbor_socket = new HashMap<>();
	private ServerSocket ss;

	public TCPDriver(Component c) {
		super(c);
	}

	@Override
	public String getProtocolName() {
		return "TCP";
	}

	@Override
	public boolean canContact(Component c) {
		return super.canContact(c) && c.info.tcpPort != null;
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
					var from = msg.route.last().transport().component;

					synchronized (neighbor_socket) {
						Entry e = neighbor_socket.get(from);

						if (e == null || e.socket != socket) {
							neighbor_socket.put(from, new Entry(socket));
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
		synchronized (neighbor_socket) {
			Iterator<java.util.Map.Entry<Component, Entry>> i = neighbor_socket.entrySet().iterator();

			while (i.hasNext()) {
				java.util.Map.Entry<Component, Entry> e = i.next();

				if (e.getValue().socket == s) {
					i.remove();
				}
			}
		}

		try {
			s.close();
		} catch (IOException e1) {
		}
	}

	@Override
	protected void multicastImpl(Message msg, Collection<OutNeighbor> neighbors) {
		for (var n : neighbors) {
			Entry entry = null;

			synchronized (neighbor_socket) {
				entry = neighbor_socket.get(n);

				// there is no connection to this peer yet
				// try to establish one
				if (entry == null) {
					entry = createSocket(n.dest.component);
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

	@Override
	protected void bcastImpl(Message msg) {
		for (var entry : neighbor_socket.values()) {
			try {
				serializer.write(msg, entry.os);
			} catch (IOException e) {
				errorOn(entry.socket);
			}
		}
	}

	protected Entry createSocket(Component to) {
		for (var ip : to.info.inetAddresses) {
			Entry entry = null;

			try {
				var socket = new Socket(ip, to.info.tcpPort);
				neighbor_socket.put(to, entry = new Entry(socket));
				newSocket(socket);
				return entry;

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

		return null;
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
	public Collection<Component> actualNeighbors() {
		return neighbor_socket.keySet();
	}
}