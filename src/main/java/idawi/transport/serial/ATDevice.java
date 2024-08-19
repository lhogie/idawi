package idawi.transport.serial;

import com.fazecast.jSerialComm.SerialPort;

import toools.thread.Threads;

public class ATDevice extends SerialDevice {
	private SetUpMode setup;

	public ATDevice(SerialPort p) {
		super(p);
	}

	public synchronized SetUpMode setup() {
		if (setup != null)
			throw new IllegalStateException("already in setup mode");

		setup = new SetUpMode(this);
		Threads.sleep(1.1);
		setup.out.print("+++");
		Threads.sleep(1.1);
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
		setup.awaitingMessages.forEach(b -> bcast(b));
		setup = null;
	}
}
