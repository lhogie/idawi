package idawi.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import toools.io.Cout;
import toools.math.MathsUtilities;

public class UDPDriver extends IPDriver implements Broadcastable {
	// public static final int DEFAULT_PORT = IPDriver.DEFAULT_PORT;
	public List<Integer> broadcastPorts = new ArrayList<>();

	private DatagramSocket socket;

	public UDPDriver(Component c) {
		super(c);
	}

	@Override
	public String getProtocolName() {
		return "UDP";
	}

	@Override
	protected void serveLoop() {
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
						 Cout.info("UDP received " + msg);
						// Cout.debugSuperVisible(msg.ID);
						processIncomingMessage(msg);
					} catch (IOException err) {
						err.printStackTrace();
					}
				}
			} catch (IOException err) {
				err.printStackTrace();
				// Threads.sleepMs(1000);
			}
		}
	}

	@Override
	public void setPort(int port) {
		broadcastPorts.add(port);
		super.setPort(port);
	}

	@Override
	protected void stopServer() {
		socket.close();
		socket = null;
		port = -1;
	}

	@Override
	public void dispose(Link l) {
	}

	@Override
	public double latency() {
		return MathsUtilities.pickRandomBetween(0.000010, 0.000030, Idawi.prng);
	}

	@Override
	public void bcast(byte[] msgBytes) {
		try {
			for (var ni : NetworkInterface.networkInterfaces().toList()) {
				for (int p : broadcastPorts) {
					for (var ia : ni.getInterfaceAddresses()) {
						var ba = ia.getBroadcast();

						if (ba != null) {
							udpSend(msgBytes, ba, p);
						}
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void multicast(byte[] msgBytes, Collection<Link> outLinks) {
		for (var l : outLinks) {
			var i = l.dest.component.dt().info();
			udpSend(msgBytes, i.inetAddresses.get(0), i.udpPort);
		}
	}

	private void udpSend(byte[] buf, InetAddress ip, int port) {
		var p = new DatagramPacket(buf, buf.length);
		p.setAddress(ip);
		p.setPort(port);

		try {
			Cout.debug("udp send on port " +  port);
			socket.send(p);
		} catch (IOException e1) {
		}
	}
}
