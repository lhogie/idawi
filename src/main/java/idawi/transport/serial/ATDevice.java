package idawi.transport.serial;

import com.fazecast.jSerialComm.SerialPort;

import java.io.PrintStream;

public class ATDevice extends SerialDevice {
	private SetUpMode setup;
	private PrintStream ps;
	/**
	 * Très important de ne pas faire un println au lieu d'un print car le separator
	 * d'un OS à l'autre n'est pas le même et AT ne marche qu'avec le separator \r\n
	 */
	String separator = "\r\n";

	public ATDevice(SerialPort p) {
		super(p);
	}

	synchronized SetUpMode setup() {
		if (setup != null)
			throw new IllegalStateException("already in setup mode");

		setupping = true;
		setup = new SetUpMode(this);
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

		setup.out.print("ATO");
		setup.out.print(separator);

		setupping = false;
		setup.awaitingMessages.forEach(b -> bcast(b));
		setup = null;
	}

	/**
	 * Fonctionne exactement comme la fonction existSetup plus mais fais un reload
	 * au lieu de simplement quitter le setup
	 */
	synchronized void exitSetupReload() {

		if (setup == null)
			throw new IllegalStateException("not in setup mode");

		setup.out.print("ATZ");
		setup.out.print(separator);
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
