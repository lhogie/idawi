package idawi.transport.serial;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;

import toools.thread.Q;

public class SetUpMode {
	PrintStream out;
	BufferedReader in;
	private final ATDevice device;
	Q<byte[]> awaitingMessages = new Q<>(100);

	SetUpMode(ATDevice d) {
		this.device = d;
		out = new PrintStream(d.serialPort.getOutputStream());
		in = new BufferedReader(new InputStreamReader(d.ser));
	}


	public void exit() {
		device.exitSetup();
	}

	public Config getConfig() {
		Config config;
		try {

			var ps = enterSetupMode();
			ps.out.println("ATI5");

			System.out.println("avant poll");
			System.out.println(serialPort.isOpen());

			System.out.println(serialPort.getInputStream());
			config = configQ.poll_sync(2);

			System.out.println("après poll");
			ps.out.println("ATO");

			return config;
		} catch (Exception e) {
			e.printStackTrace();
		}
		config = new Config();
		return config;
	}

	public Config setConfig(Config c) {
		try {
			System.out.println("begin Set Config");
			var ps = enterSetupMode();
			for (Param param : c) {
				if (param.code == "S0") {
					continue;
				}
				ps.println("AT" + param.code + "=" + param.value);
				Thread.sleep(100);

			}

			// save and reboot
			ps.println("AT&W");

			ps.println("ATZ");
			rebooting = true;

			// block 10s until rebooted
			var rebootAknowlgement = rebootQ.poll_sync(2);
			System.out.println("reboot aknow :" + rebootAknowlgement);
			rebooting = false;
			System.out.println("end Set Config");

		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}

		return getConfig();
	}
}
