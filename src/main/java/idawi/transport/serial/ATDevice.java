package idawi.transport.serial;

import java.io.IOException;
import java.io.PrintStream;

import com.fazecast.jSerialComm.SerialPort;

import toools.thread.Q;

public class ATDevice extends SerialDevice {

	public ATDevice(SerialPort p) {
		super(p);

	}

	@Override
	public String toString() {
		return getConfig().toString();
	}

	private PrintStream enterSetupMode() {
		try {
			var ps = new PrintStream(serialPort.getOutputStream());
			Thread.sleep(1100);
			ps.print("+++");
			Thread.sleep(1100);

			return ps;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Config getConfig() {
		Config config;
		try {

			var ps = enterSetupMode();
			ps.println("ATI5");

			System.out.println("avant poll");
			System.out.println(serialPort.isOpen());

			System.out.println(serialPort.getInputStream());
			config = configQ.poll_sync(2);

			System.out.println("après poll");
			ps.println("ATO");

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
