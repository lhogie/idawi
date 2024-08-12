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
		var ps = enterSetupMode();
		ps.println("ATI5");

		System.out.println("avant poll");

		var config = configQ.poll_sync(1000);
		System.out.println("après poll");
		ps.println("ATO");

		return config;
	}

	public Config setConfig(Config c) {
		try {
			System.out.println("yo");
			var ps = enterSetupMode();
			ps.print(c.stream().map(param -> "ATS" + param.code + "=" + param.value)
					.reduce((a, b) -> a + "\n" + b).get());
			Thread.sleep(1000);

			// save and reboot
			rebooting = true;
			ps.println("AT&W");
			ps.println("ATZ");

			// block 10s until rebooted
			rebootQ.poll_sync(10);
			rebooting = false;
			System.out.println("nice");

		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}

		return getConfig();
	}
}
