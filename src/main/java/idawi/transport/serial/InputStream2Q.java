package idawi.transport.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import toools.thread.Q;

public class InputStream2Q implements Runnable {
	private final Ok okSingleton = new Ok();
	public final Q q = new Q(1);
	private final InputStream in;

	public InputStream2Q(InputStream in) {
		this.in = in;
	}

	@Override
	public void run() {
		while (true) {
			try {
				int i = in.read();

				if (i == -1) {
					q.add_sync(new EOF());
					return;
				} else {
					okSingleton.b = (byte) i;
					q.add_sync(okSingleton);
				}
			} catch (IOException err) {
				q.add_sync(err);
			}
		}
	}

	public static class Ok {
		byte b;
	}

	public static class EOF {

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
			var regexString = "S15:.*[\r\n]+";
			var ps = enterSetupMode();
			ps.println("ATI5");

			System.out.println("avant poll");
			config = readConfig(regexString);
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

			// block 10s until rebooted
			var rebootAknowlgement = q.poll_sync(2);
			System.out.println("reboot aknow :" + rebootAknowlgement);
			System.out.println("end Set Config");

		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}

		return getConfig();
	}

	private Config readConfig(String reString) {
		var buf = new MyByteArrayOutputStream();
		var inputStream = in;
		var c = new Config();
		try {

			while (true) {
				if ((inputStream.available() == 0) && buf.endsByData(reString)) {
					return dataParse(buf.toByteArray());

				}
				int i = inputStream.read();
				if (i == -1) {
					return c;
				}

				buf.write((byte) i);

			}
		} catch (IOException err) {
			System.err.println("I/O error reading stream");
		}
		return c;
	}

	public Config dataParse(byte[] bytes) {

		var config = Config.from(new String(bytes));
		return config;

	}

}
