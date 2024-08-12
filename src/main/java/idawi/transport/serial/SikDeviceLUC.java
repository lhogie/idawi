package idawi.transport.serial;

import com.fazecast.jSerialComm.SerialPort;

public class SikDeviceLUC extends ATDevice {

	public SikDeviceLUC(SerialPort p) {
		super(p);

	}

	public void initialConfig() {

		var config = getConfig();
		System.out.println(config);
		config = getConfig();
		System.out.println(config);

		config.findByName("TXPOWER").value = 40;
		setConfig(config);
	}
}
