package idawi.transport.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

import idawi.Component;
import idawi.Idawi;
import idawi.transport.Broadcastable;
import idawi.transport.Link;
import idawi.transport.TransportService;

public class SerialDriver extends TransportService implements Broadcastable {

	private static List<SerialDevice> devices = new ArrayList<>();

	public SerialDriver(Component c) {
		super(c);
		Idawi.agenda.threadPool.submit(() -> {
			try {
				updateDeviceList();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}

	public SerialDevice getDeviceUsing(SerialPort port) {
		for (var d : devices) {
			if (d.getName().equalsIgnoreCase(port.getDescriptivePortName())) {
				return d;
			}
		}

		return null;
	}

	public synchronized void updateDeviceList() throws InterruptedException {
		while (true) {
			createDevicesForNewPorts(SerialPort.getCommPorts());
			removeDeviceForDisapearedPorts(SerialPort.getCommPorts());
			Thread.sleep(1000);
		}
	}

	private synchronized void removeDeviceForDisapearedPorts(SerialPort[] serialPorts) {
		for (SerialDevice device : new ArrayList<>(devices)) {
			if (Arrays.stream(serialPorts).filter(p -> p.equals(device.serialPort)).findAny().isEmpty()) {
				devices.remove(device);
			}
		}
	}

	private synchronized void createDevicesForNewPorts(SerialPort[] serialPorts) {
		for (var serialPort : serialPorts) {
			if (!serialPort.isOpen() && getDeviceUsing(serialPort) == null) {
				if ((!serialPort.getDescriptivePortName().contains("Bluetooth"))
						&& (!serialPort.getDescriptivePortName().contains("S4"))) {
					serialPort.openPort();
					serialPort.setBaudRate(115200);// configurable ?
					serialPort
							.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);// configurable
																														// ?
					serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);

					var device = isSIK(serialPort) ? new SikDevice(serialPort) : new SerialDevice(serialPort);
					device.newThread(this);
					System.out.println("okay for fetch");
					devices.add(device);
				}
			}
		}
	}

	private boolean isSIK(SerialPort p) {
		byte[] sikMarkerVerifier = "ATI".getBytes();
		p.writeBytes(sikMarkerVerifier, sikMarkerVerifier.length);
		var buf = new MyByteArrayOutputStream();

		while (true) {
			int i;
			try {
				i = p.getInputStream().read();
				if (i == -1) {
					buf.close();
					return false;
				}
				buf.write((byte) i);
				if (buf.endsBy(sikMarkerVerifier)) {
					buf.close();
					return true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	@Override
	protected void multicast(byte[] msgBytes, Collection<Link> outLinks) {
		bcast(msgBytes);
	}

	@Override
	public void bcast(byte[] msgBytes) {
		for (var d : devices) {
			d.bcast(msgBytes);
		}
	}

	@Override
	public String getName() {
		return "serial port driver";
	}

	@Override
	public void dispose(Link l) {
	}

	@Override
	public double latency() {
		return -1;
	}

}