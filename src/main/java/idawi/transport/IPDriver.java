package idawi.transport;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import idawi.Component;
import toools.io.Cout;
import toools.thread.Q;

public abstract class IPDriver extends TransportService {
	public static final int DEFAULT_PORT = 4553;

	protected int port = -1;
	private Thread thread;
	private final Q waitReady = new Q(1);

	public IPDriver(Component c) {
		super(c);
	}

	protected void markReady() {
		Cout.info(getName() + " is ready, port=" + getPort());
		waitReady.add_sync("ready");
	}

	@Override
	public String getName() {
		return getProtocolName();
	}

	protected abstract String getProtocolName();

	public int getPort() {
		return port;
	}

	public void setPort(int newPort) {
		if (newPort == port)
			return;

		if (port > 0) {
			stopServer();

			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		this.port = newPort;

		if (newPort > 0) {
			thread = new Thread(() -> serveLoop());
			thread.start();

			// and waits that the server actually listens
			if (waitReady.pollOrFail_sync(10) == null) {
				Cout.debug(this + "'s server not started");
			}
		}
	}

	protected final boolean isRunning() {
		return port >= 0;
	}

	protected abstract void stopServer();

	protected abstract void serveLoop();

	public static boolean isThisMyIpAddress(InetAddress addr) {
		// Check if the address is a valid special local or loop back
		if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
			return true;

		// Check if the address is defined on any interface
		try {
			return NetworkInterface.getByInetAddress(addr) != null;
		} catch (SocketException e) {
			return false;
		}
	}

	@Override
	public String toString() {
		return super.toString() + ":" + port;
	}
}
