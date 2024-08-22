package idawi.transport.serial;

import com.fazecast.jSerialComm.SerialPort;

public class SikDeviceLUC extends ATDevice {

	public SikDeviceLUC(SerialPort p) {
		super(p);

	}

	public void initialConfig() {
		setup();
		var config = getConfig();
		if (config != null)
			System.out.println("config 1: correct");
		setup();

		config = getConfig();
		if (config != null)

			System.out.println("config 2: correct");

		// config.findByName("TXPOWER").value = 5;
		setup();
		config = setConfig(config);
		if (config != null)
			System.out.println("config 3: correct");
	}
}
