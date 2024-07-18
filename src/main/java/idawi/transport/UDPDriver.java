package idawi.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import toools.io.Cout;
import toools.math.MathsUtilities;

public class UDPDriver extends IPDriver implements Broadcastable {
	// public static final int DEFAULT_PORT = IPDriver.DEFAULT_PORT;
	private DatagramSocket socket;

	public UDPDriver(Component c) {
		super(c);

		if (!c.isDigitalTwin()) {
			Idawi.agenda.threadPool.submit(() -> startServer());
		}
	}

	@Override
	public String getProtocolName() {
		return "UDP";
	}

	@Override
	protected void startServer() {
		byte[] buf = new byte[1000000];

		while (true) {
			try {
				if (socket != null) {
					socket.close();
				}

				socket = new DatagramSocket(getPort());
				markReady();

				while (true) {
					DatagramPacket p = new DatagramPacket(buf, buf.length);

					try {
						// Cout.info("reading packet");
						socket.receive(p);
						Message msg = (Message) serializer.fromBytes(p.getData());
						// Cout.info("UDP received " + msg);
						// Cout.debugSuperVisible(msg.ID);
						processIncomingMessage(msg);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				// Threads.sleepMs(1000);
			}
		}
	}

	@Override
	protected void stopServer() {
		socket.close();
		socket = null;
	}

	@Override
	protected boolean isRunning() {
		return socket != null;
	}

	@Override
	public void dispose(Link l) {
	}

	@Override
	public double latency() {
		return MathsUtilities.pickRandomBetween(0.000010, 0.000030, Idawi.prng);
	}

	@Override
	protected void multicast(byte[] msgBytes, Collection<Link> outLinks) {
		for (var l : outLinks) {
			var i = l.dest.component.dt().info();
			udpSend(msgBytes, i.inetAddresses.get(0), i.udpPort);
		}
	}

	private void udpSend(byte[] buf, InetAddress ip, int port) {
		DatagramPacket p = new DatagramPacket(buf, buf.length);
		p.setAddress(ip);
		p.setPort(getPort());
		Cout.debugSuperVisible(ip);
		try {
			socket.send(p);
		} catch (IOException e1) {
		}
	}

	@Override
	public final void bcast(byte[] msgBytes) {
		try {
			for (var ni : NetworkInterface.networkInterfaces().toList()) {
				for (var ia : ni.getInterfaceAddresses()) {
					InetAddress baddr = ia.getBroadcast();

					if (baddr != null) {
						for (int port : bcastPortList) {
							udpSend(msgBytes, baddr, port);
						}
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
}
