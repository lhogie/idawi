package idawi.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import idawi.ComponentInfo;
import idawi.TransportLayer;
import toools.io.Cout;
import toools.net.NetUtilities;
import toools.thread.Q;

public abstract class IPDriver extends TransportLayer {
//	public static final int DEFAULT_PORT = 4553;

	private int port = NetUtilities.randomUserPort();
	private Thread thread;
	private final Q waitReady = new Q(1);

	protected void markReady() {
		Cout.info(getName() + " is ready");
		waitReady.add_blocking("ready");
	}

	@Override
	public String getName() {
		return getProtocolName();
	}

	protected abstract String getProtocolName();

	@Override
	protected void start() {
		if (isRunning())
			throw new IllegalStateException("already running");

		if (port > 0) {
			// start the server in a separate thread
			thread = new Thread(() -> startServer());
			thread.start();

			// and waits that the server actually listens
			waitReady.get_blockingOrFail(1000000);
		}
	}

	@Override
	public boolean canContact(ComponentInfo c) {
		return !c.inetAddresses.isEmpty();
	}

	@Override
	protected void stop() {
		stopServer();

		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;

		if (isRunning()) {
			stop();

			if (port > 0) {
				start();
			}
		}
	}

	protected abstract boolean isRunning();

	protected abstract void stopServer();

	protected abstract void startServer();

	@Override
	public void injectLocalInfoTo(ComponentInfo c) {
		try {
			c.inetAddresses.add(InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

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
