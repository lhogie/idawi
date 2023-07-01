package idawi.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Collection;

import idawi.Component;
import idawi.messaging.Message;
import idawi.service.local_view.LocalViewService;

public class UDPDriver extends IPDriver {
	// public static final int DEFAULT_PORT = IPDriver.DEFAULT_PORT;
	private DatagramSocket socket;

	public UDPDriver(Component c) {
		super(c);
	}

	@Override
	public String getProtocolName() {
		return "UDP";
	}

	@Override
	protected void sendImpl(Message msg) {
		if (socket == null)
			return;

		byte[] buf = serializer.toBytes(msg);
		// Cout.debugSuperVisible("sending to " + neighbors);

		// System.out.println(n.toHTML());

		var to = msg.route.last().link.dest;

		if (msg.route.isEmpty())
			throw new IllegalStateException("empty route");

		DatagramPacket p = new DatagramPacket(buf, buf.length);
		p.setAddress(to.component.dt().info().inetAddresses.get(0));
		// System.out.println(relay.inetAddresses.get(0) + ":" +
		// relay.udpPort);
		p.setPort(to.component.dt().info().udpPort);

		try {
			socket.send(p);
			// System.out.println(n.inetAddresses.get(0));
			// System.out.println(n.udpPort);
		} catch (IOException e1) {
		}

	}

	@Override
	public boolean canContact(Component c) {
		return super.canContact(c) && c.dt().info().udpPort != null;
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
						component.services(LocalViewService.class).forEach(s -> s.feedWith(msg.route));
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
}
