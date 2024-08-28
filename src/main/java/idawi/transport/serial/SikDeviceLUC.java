package idawi.transport.serial;

import com.fazecast.jSerialComm.SerialPort;

public class SikDeviceLUC extends ATDevice {
	boolean firstConfig = false;

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

		config = defaultConfig(config);
		if (config != null)
			System.out.println("config 3: correct");
		System.out.println("setupping :" + setupping);
		firstConfig = true;
	}

	public Config defaultConfig(Config c) {
		var config = c;
		config.findByName("SERIAL_SPEED").value = 115;
		config.findByName("AIR_SPEED").value = 128;
		config.findByName("NETID").value = 25;
		config.findByName("TXPOWER").value = 5;
		config.findByName("ECC").value = 0;
		config.findByName("MAVLINK").value = 0;
		config.findByName("OPPRESEND").value = 1;
		config.findByName("MIN_FREQ").value = 433050;
		config.findByName("MAX_FREQ").value = 434790;
		config.findByName("NUM_CHANNELS").value = 10;
		config.findByName("DUTY_CYCLE").value = 100;
		config.findByName("LBT_RSSI").value = 0;
		config.findByName("MANCHESTER").value = 0;
		config.findByName("RTSCTS").value = 1;
		config.findByName("MAX_WINDOW").value = 131;
		setup();
		return setConfig(config);
	}
}
