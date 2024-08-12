package idawi.transport.serial;

import java.io.IOException;
import java.util.ArrayList;
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
				while (true) {
					updateDeviceList();
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}

	public synchronized void updateDeviceList() {
		createDevicesForNewPorts(SerialPort.getCommPorts());
		removeDeviceForDisapearedPorts(List.of(SerialPort.getCommPorts()));
	}

	private synchronized void removeDeviceForDisapearedPorts(List<SerialPort> serialPorts) {
		for (var device : List.copyOf(devices)) {
			boolean portStillExists = serialPorts.contains(device.serialPort);

			if (!portStillExists && !device.rebooting) {
				devices.remove(device);
			}
		}
	}

	private synchronized void createDevicesForNewPorts(SerialPort[] serialPorts) {
		for (var serialPort : serialPorts) {

			// search for a device with the same port name

			var deviceFirst = devices.stream().filter(
					d -> d.serialPort.getDescriptivePortName().equalsIgnoreCase(serialPort.getDescriptivePortName()))
					.findFirst();
			SerialDevice device;

			if (deviceFirst.isPresent()) {

				device = deviceFirst.get();
			} else {
				device = null;

			}

			if (device == null) {
				open(serialPort);

				device = isSIK(serialPort) ? new SikDeviceLUC(serialPort) : new SerialDevice(serialPort);

				devices.add(device);

				device.newThread(this);
				if (device instanceof SikDeviceLUC sd) {
					sd.initialConfig();

				}

			} else if (!serialPort.isOpen()) {
				open(serialPort);
				device.serialPort = serialPort;

				if (device.rebootQ != null) {
					device.rebootQ.add_sync(new Object());
				}
			}
		}
	}

	private void open(SerialPort serialPort) {
		serialPort.openPort();
		serialPort.setBaudRate(115200);// configurable ?
		serialPort.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);// configurable
		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
	}

	private boolean isSIK(SerialPort p) {
		byte[] setupMarker = "+++".getBytes();
		byte[] sikMarker = "ATI".getBytes();
		byte[] outMarker = "ATO".getBytes();
		byte[] separator = System.getProperty("line.separator").getBytes();
		p.writeBytes(setupMarker, setupMarker.length);
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

				if (buf.endsBy("OK".getBytes())) {
					buf.reset();
					p.writeBytes(sikMarker, sikMarker.length);
					p.writeBytes(separator, separator.length);

				} else if (buf.endsBy("SiK".getBytes()) || buf.endsBy("sik".getBytes())
						|| buf.endsBy("SIK".getBytes())) {
					p.writeBytes(outMarker, outMarker.length);
					p.writeBytes(separator, separator.length);
					p.getInputStream().readNBytes(p.bytesAvailable());
					System.out.println(p.bytesAvailable());
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