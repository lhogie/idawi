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
import idawi.Idawi;
import idawi.messaging.Message;
import toools.math.MathsUtilities;
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
					Message msg = (Message) component.secureSerializer.read(is);
					var from = msg.route.last().link.src.component;

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
	protected void sendImpl(Message msg) {
		Entry entry = null;
		TransportService n = msg.route.last().link.dest;

		synchronized (neighbor_socket) {
			entry = neighbor_socket.get(n);

			// there is no connection to this peer yet
			// try to establish one
			if (entry == null) {
				entry = createSocket(n.component);
			}
		}

		// if a connection could be obtained
		if (entry != null) {
			try {
				component.secureSerializer.write(msg, entry.os);
			} catch (IOException e) {
				errorOn(entry.socket);
			}
		}
	}

	protected Entry createSocket(Component to) {
		for (var ip : to.dt().info().inetAddresses) {
			Entry entry = null;

			try {
				var socket = new Socket(ip, to.dt().info().tcpPort);
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

	public Collection<Component> actualNeighbors() {
		return neighbor_socket.keySet();
	}

	@Override
	public void dispose(Link l) {
		try {
			neighbor_socket.get(l.dest.component).socket.close();
		} catch (IOException err) {
		}
	}
	
	@Override
	public double latency() {
		return MathsUtilities.pickRandomBetween(0.000020, 0.000060, Idawi.prng);
	}

}
