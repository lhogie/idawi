package idawi.transport.serial;

import java.io.IOException;
import java.io.PrintStream;

import com.fazecast.jSerialComm.SerialPort;

import toools.thread.Q;

public class ATDevice extends SerialDevice {
	Q<Config> configQ = new Q<>(1);

	public ATDevice(SerialPort p) {
		super(p);

		markers.add(new Callback() {
			@Override
			public byte[] marker() {
				return "ATO".getBytes();
			}

			@Override
			public void callback(byte[] bytes, SerialDriver d) {
				System.out.println("callback AT :" + new String(bytes));
				configQ.add_sync(Config.from(new String(bytes)));
			}
		});
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
		for (Config config : configQ) {
			System.out.println("config : " + config);
		}
		System.out.println("what");

		var config = configQ.poll_sync(1000);
		System.out.println("OUAAAAAAAAAAAAAIS");

		ps.println("ATO");
		return config;
	}

	public Config setConfig(Config c) {
		try {
			enterSetupMode().print(c.stream().map(param -> "ATS" + param.code + "=" + param.value)
					.reduce((a, b) -> a + "\n" + b).get());
			Thread.sleep(1000);

			// save and reboot
			rebooting = true;
			serialPort.getOutputStream().write("AT&W\nATZ\n".getBytes());

			// block 10s until rebooted
			rebootQ.poll_sync(10);
			rebooting = false;
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException(e);
		}

		return getConfig();
	}
}
