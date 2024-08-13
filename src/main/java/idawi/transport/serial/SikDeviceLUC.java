package idawi.transport.serial;

import com.fazecast.jSerialComm.SerialPort;

public class SikDeviceLUC extends ATDevice {

	public SikDeviceLUC(SerialPort p) {
		super(p);

	}

	public void initialConfig() {

		var config = getConfig();
		if (config != null)
			System.out.println("config 1: correct");

		config = getConfig();
		if (config != null)

			System.out.println("config 2: correct");

		// config.findByName("TXPOWER").value = 5;
		config = setConfig(config);
		System.out.println("config 3:" + config);
	}
}
