package idawi.transport.serial;

import com.fazecast.jSerialComm.SerialPort;

public class SikDeviceLUC extends ATDevice {
	
	public SikDeviceLUC(SerialPort p) {
		super(p);
		var config = getConfig();
		config.findByName("TXPOWER").value = 40;
		setConfig(config);
	}
}
