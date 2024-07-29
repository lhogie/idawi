package idawi.transport;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import com.fazecast.jSerialComm.SerialPort;

import idawi.Component;
import idawi.Idawi;

public class JSerialCommTransport extends StreamBasedDriver implements Broadcastable {
	public static ArrayList<SerialPort> serialOpen = new ArrayList<>(); // Create an ArrayList object

	public JSerialCommTransport(Component c) {
		super(c);

		Idawi.agenda.threadPool.submit(() -> {
			try {
				openPorts();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});

	}

	@Override
	public String getName() {
		return "serial ports";
	}

	@Override
	public void dispose(Link l) {
		// l.activity.close();
	}

	public boolean checkOpenArray(SerialPort serialPort) {

		for (SerialPort serialPortOpen : serialOpen) {
			if (serialPortOpen.getDescriptivePortName().equalsIgnoreCase(serialPort.getDescriptivePortName())) {

				return true;

			}

		}
		return false;

	}

	public void openPorts() throws InterruptedException {
		boolean serialOpenContains = false;

		while (true) {
			SerialPort[] allPorts = SerialPort.getCommPorts();

			for (SerialPort serialPort : allPorts) {
				serialOpenContains = checkOpenArray(serialPort);
				if (!serialPort.isOpen() && !serialOpenContains) {
					if (!serialPort.getDescriptivePortName().contains("Bluetooth")) {
						serialPort.openPort();
						serialPort.setBaudRate(57600);
						serialPort.setFlowControl(
								SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);
						serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);

						serialOpen.add(serialPort);
					}
				}
			}
			threadAllocator();

			Thread.sleep(1000);

		}
	}

	@Override
	public double latency() {
		return 0;
	}

	@Override
	protected Stream<InputStream> inputStreams() {
		return Arrays.stream(serialOpen.toArray()).map(p -> ((SerialPort) p).getInputStream());
	}

	@Override
	protected Stream<OutputStream> outputStreams() {
		return Arrays.stream(serialOpen.toArray()).map(p -> ((SerialPort) p).getOutputStream());
	}

}
