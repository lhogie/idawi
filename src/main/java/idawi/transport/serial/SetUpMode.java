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

			out.print("ATI5");
			out.print(device.separator);

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

		}
		return null;
	}

	Config setConfig(Config c) {
		try {
			for (Param param : c) {
				if (param.code.equals("S0")) {
					continue;
				}

				out.print("AT" + param.code + "=" + param.value);
				out.print(device.separator);

				okDetector();

			}
			// save and reboot
			out.print("AT&W");
			out.print(device.separator);

			okDetector();

			exitReload();
			// block 10s until rebooted

			// var rebootAknowlgement = awaitingMessages.poll_sync(2);// make a queue poll
			var rebootAknowlgement = device.rebootQ.poll_sync(2);
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
		byte[] currentByte = new byte[1];

		SerialPort p = device.serialPort;
		p.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000,
				1000);
		try {

			while (true) {
				int i = p.readBytes(currentByte, 1); // j'utilise readBytes de JserialComm car son timeout peut être
														// gérer par la fonction setComPortTimeouts un peu plus haut

				if (i == -1) {
					buf.close();
					return c;
				}

				buf.write((byte) currentByte[0]);
				if ((in.available() == 0) && buf.endsByData(reString)) {
					p.setComPortTimeouts(
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
		byte[] currentByte = new byte[1];

		SerialPort p = device.serialPort;
		p.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000,
				1000);
		try {

			while (true) {
				int i = p.readBytes(currentByte, 1); // j'utilise readBytes de JserialComm car son timeout peut être
														// gérer par la fonction setComPortTimeouts un peu plus haut

				if (i == -1) {
					buf.close();
					return false;
				}

				buf.write((byte) currentByte[0]);
				if ((in.available() == 0) && buf.endsByData("OK")) {
					p.setComPortTimeouts(
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
