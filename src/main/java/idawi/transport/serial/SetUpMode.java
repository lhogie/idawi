package idawi.transport.serial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import com.fazecast.jSerialComm.SerialPort;

import toools.thread.Q;

public class SetUpMode {
	PrintStream out;
	InputStream in;
	private final ATDevice device;
	Q<byte[]> awaitingMessages = new Q<>(100);

	SetUpMode(ATDevice d) {
		this.device = d;

		out = new PrintStream(d.serialPort.getOutputStream());
		in = d.serialPort.getInputStream();
	}

	private void exit() {
		device.exitSetup();
	}

	private void exitReload() {
		device.exitSetupReload();
	}

	Config getConfig() {
		Config config;
		try {
			var regexString = "S15:.*[\r\n]+";

			out.println("ATI5");

			config = readConfig(regexString);
			exit();

			return config;
		} catch (Exception e) {
			e.printStackTrace();
		}
		config = new Config();
		return config;
	}

	PrintStream enterSetupMode() {
		try {

			Thread.sleep(1100);
			out.print("+++");
			Thread.sleep(1100);

			return out;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	Config setConfig(Config c) {
		try {
			for (Param param : c) {
				if (param.code.equals("S0")) {
					continue;
				}

				out.println("AT" + param.code + "=" + param.value);
				okDetector();

			}
			// save and reboot
			out.println("AT&W");
			okDetector();
			exitReload();

			// block 10s until rebooted
			var rebootAknowlgement = awaitingMessages.poll_sync(2);// make a queue poll

			device.rebooting = false;
			// System.out.println("reboot aknow :" + rebootAknowlgement);
			device.setup();
			return getConfig();

		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

	}

	private Config readConfig(String reString) {
		var buf = new MyByteArrayOutputStream();
		var c = new Config();
		device.serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000,
				1000);

		try {

			while (true) {
				int i;
				i = in.read();// timeout not working because the timeout should be on the read and write, this
								// is not SIK specific but java specific ask Luc
				if (i == -1) {
					buf.close();
					return c;
				}

				buf.write((byte) i);
				if ((in.available() == 0) && buf.endsByData(reString)) {
					device.serialPort.setComPortTimeouts(
							SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
							0, 0);
					return dataParse(buf.toByteArray());

				}

			}
		} catch (IOException err) {
			System.err.println("I/O error reading stream");
		}
		return c;
	}

	private boolean okDetector() {
		var buf = new MyByteArrayOutputStream();
		device.serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000,
				1000);
		try {

			while (true) {
				int i;
				i = in.read(); // timeout not working because the timeout should be on the read and write, this
								// is not SIK specific but java specific ask Luc
				if (i == -1) {
					buf.close();
					return false;
				}

				buf.write((byte) i);
				if ((in.available() == 0) && buf.endsByData("OK")) {
					device.serialPort.setComPortTimeouts(
							SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
							0, 0);
					return true;

				}

			}
		} catch (IOException err) {
			System.err.println("I/O error reading stream");
		}
		return false;
	}

	private Config dataParse(byte[] bytes) {

		var config = Config.from(new String(bytes));
		return config;

	}
}
