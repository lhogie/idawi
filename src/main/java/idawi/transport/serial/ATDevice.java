package idawi.transport.serial;

import com.fazecast.jSerialComm.SerialPort;

import java.io.PrintStream;

public class ATDevice extends SerialDevice {
	private SetUpMode setup;
	private PrintStream ps;

	public ATDevice(SerialPort p) {
		super(p);
	}

	synchronized SetUpMode setup() {
		if (setup != null)
			throw new IllegalStateException("already in setup mode");

		setup = new SetUpMode(this);
		System.out.println("setup AT done");
		// Threads.sleep(1.1);
		// setup.out.print("+++");
		// Threads.sleep(1.1);
		ps = setup.enterSetupMode();
		return setup;
	}

	public synchronized void bcast(byte[] msgBytes) {
		if (setup != null) {
			setup.awaitingMessages.add_sync(msgBytes);
		} else {
			super.bcast(msgBytes);
		}
	}

	synchronized void exitSetup() {
		if (setup == null)
			throw new IllegalStateException("not in setup mode");

		setup.out.println("ATO");
		setupping = false;
		setup.awaitingMessages.forEach(b -> bcast(b));
		setup = null;
	}

	synchronized void exitSetupReload() {
		if (setup == null)
			throw new IllegalStateException("not in setup mode");

		setup.out.println("ATZ");

		setupping = false;
		setup.awaitingMessages.forEach(b -> bcast(b));
		rebooting = true;
		setup = null;
	}

	synchronized Config getConfig() {
		return setup.getConfig();
	}

	synchronized Config setConfig(Config c) {
		return setup.setConfig(c);
	}
}
