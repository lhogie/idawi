package idawi.transport.serial;

import java.io.IOException;

import com.fazecast.jSerialComm.SerialPort;

public class SikDevice extends SerialDevice {
	private static String lineSeparator = System.getProperty("line.separator");
	public String infosParsed = "";
	public boolean getFirstConfig = true;
	Config config;

	public SikDevice(SerialPort p) {
		super(p);

		markers.add(new Callback() {

			@Override
			public byte[] marker() {
				return "ATO".getBytes();
			}

			@Override
			public void callback(byte[] bytes, SerialDriver d) {
				config = new Config();

				for (var s : new String(bytes).split("\\n")) {
					String[] splitString = s.split(":|=");
					var code = splitString[0].replaceAll("[^\\d.]", "");
					var name = splitString[1];
					var value = Integer.parseInt(splitString[2].trim());
					config.add(new Param(code, name, value));
				}
			}
		});

		showSetup();
	}

	@Override
	public String toString() {
		return "Config in Sik Device : " + config;
	}

	private void enterSetupMode() throws IOException {
		serialPort.getOutputStream().write("+++".getBytes());
	}

	private void setParameter(String param, int value) throws IOException {
		try {
			serialPort.getOutputStream().write((param + "=" + value).getBytes());
			serialPort.getOutputStream().write(lineSeparator.getBytes());
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	public void showSetup() {
		try {
			enterSetupMode();
			var os = serialPort.getOutputStream();
			os.write(("ATI5").getBytes());
			os.write(lineSeparator.getBytes());
			Thread.sleep(100);
			os.write(("ATO").getBytes());
			os.write(lineSeparator.getBytes());
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean setConfig(Config c) {
		try {
			config.modifyConfig(c);
			enterSetupMode();
			for (Param param : config) {
				setParameter("ATS" + param.getCode(), param.getValue());
			}
			save();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	void save() {
		var os = serialPort.getOutputStream();

		try {
			os.write("AT&W".getBytes());
			os.write(lineSeparator.getBytes());
			Thread.sleep(100);
			os.write("ATZ".getBytes());
			os.write(lineSeparator.getBytes());
			Thread.sleep(100);

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	// public void allSetup(int serialSpeed, int airSpeed, int id, int power, int
	// ecc, int mav, int oppesend,
	// int minFreq, int maxFreq, int numChannels, int dutyCycle, int LBT, int
	// manchester, int rtscts,
	// int maxWindow) {
	// try {
	// Thread.sleep(200);
	// } catch (InterruptedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// Config cf=new Config();
	// for (Param param : config.getParams()) {
	// Param p=new Param(param.getCode(), param.getName(), )
	// cf.addParam(param);
	// }
	// setConfig(c);

	// }
}
