package idawi.transport.serial;

import java.io.IOException;

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

	private void enterSetupMode() throws IOException {
		serialPort.getOutputStream().write("+++".getBytes());
	}

	public Config getConfig() {
		try {
			enterSetupMode();
			serialPort.getOutputStream().write(("ATI5\nATO\n").getBytes());
			return configQ.poll_sync();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public Config setConfig(Config c) {
		try {
			enterSetupMode();

			for (Param param : c) {
				serialPort.getOutputStream().write(("ATS" + param.code + "=" + param.value + "\n").getBytes());
			}

			Thread.sleep(1000);
			serialPort.getOutputStream().write("AT&W\nATZ\n".getBytes());
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException(e);
		}

		return getConfig();
	}
}
