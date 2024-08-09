package idawi.transport.serial;

import com.fazecast.jSerialComm.SerialPort;

public class SikDeviceLUC extends ATDevice {

	public SikDeviceLUC(SerialPort p) {
		super(p);

		var config = getConfig();
		System.out.println("config got");

		config.findByName("TXPOWER").value = 40;
		setConfig(config);

	}
}
