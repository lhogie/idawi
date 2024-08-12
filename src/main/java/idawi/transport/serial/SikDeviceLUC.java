package idawi.transport.serial;

import com.fazecast.jSerialComm.SerialPort;

public class SikDeviceLUC extends ATDevice {

	public SikDeviceLUC(SerialPort p) {
		super(p);

	}

	public void initialConfig() {

		var config = getConfig();
		System.out.println("config 1:" + config);

		config = getConfig();
		System.out.println("config 2:" + config);

		config.findByName("TXPOWER").value = 20;
		config = setConfig(config);
		System.out.println("config 3:" + config);
	}
}
