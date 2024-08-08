package idawi.transport.serial;

import java.io.IOException;
import java.io.PrintStream;

import com.fazecast.jSerialComm.SerialPort;

import toools.thread.Q;

public class SikDeviceLUC extends SerialDevice {
	Q<Config> configQ = new Q<>(1);

	public SikDeviceLUC(SerialPort p) {
		super(p);

		markers.add(new Callback() {
			@Override
			public byte[] marker() {
				return "ATO".getBytes();
			}

			@Override
			public void callback(byte[] bytes, SerialDriver d) {
				configQ.add_sync(Config.from(new String(bytes)));
			}
		});
	}

	@Override
	public String toString() {
		return "Config in Sik Device : " + getConfig();
	}

	private PrintStream enterSetupMode() throws IOException {
		var ps = new PrintStream(serialPort.getOutputStream());
		ps.print("+++");
		return ps;
	}

	public Config getConfig() {
		try {
			enterSetupMode().print("ATI5\nATO\n");
			return configQ.poll_sync();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public Config setConfig(Config c) {
		try {
			enterSetupMode().print(c.stream().map(param -> "ATS" + param.code + "=" + param.value)
					.reduce((a, b) -> a + "\n" + b).get());
			Thread.sleep(1000);

			// save and reboot
			serialPort.getOutputStream().write("AT&W\nATZ\n".getBytes());

			// block until rebooted
			rebootWaiter.poll_sync();
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException(e);
		}

		return getConfig();
	}
}
