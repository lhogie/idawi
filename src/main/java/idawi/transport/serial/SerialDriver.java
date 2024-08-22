package idawi.transport.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

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
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}

	public synchronized void updateDeviceList() {
		SerialPort[] serialPorts = SerialPort.getCommPorts();
		createDevicesForNewPorts(serialPorts);
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
			// System.out.println("devices :" + devices);
			// System.out.println("device actuel :" + device);
			if (device == null) {
				var isOpened = open(serialPort);
				System.out.println("opened serial :" + isOpened + " " + serialPort.getDescriptivePortName());
				serialPort.addDataListener(new SerialPortDataListener() {
					@Override
					public int getListeningEvents() {
						return SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;

					}

					@Override
					public void serialEvent(SerialPortEvent serialPortEvent) {
						if (serialPortEvent.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED)
							serialPort.closePort();
						for (var device : List.copyOf(devices)) {
							boolean portStillExists = (device.serialPort == serialPort);

							if (!portStillExists && !device.rebooting) {
								devices.remove(device);
								System.out.println("device removed :" + device);
							}
						}

					}
				});
				if (isOpened) {

					device = isSIK(serialPort) ? new SikDeviceLUC(serialPort) : new SerialDevice(serialPort);

					devices.add(device);
					device.newThread(this);

					if (device instanceof SikDeviceLUC sd) {
						Idawi.agenda.threadPool.submit(() -> {
							try {

								sd.initialConfig();

							} catch (Throwable e) {
								e.printStackTrace();
							}
						});
					}
				}

			} else {
				// if (!serialPort.isOpen()) {
				// open(serialPort);
				// System.out.println("REopen :"+serialPort);
				// device.serialPort = serialPort;
				// } (opening the serialPort again when it's already being opened can lead to
				// problems like other threads thinking the port is closed (no idea why))
				if (device.rebooting) {
					device.rebootQ.add_sync(new Object());
				}
			}

		}

	}

	private boolean open(SerialPort serialPort) {
		serialPort.openPort();
		serialPort.setBaudRate(115200);// configurable ?

		serialPort.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);// configurable
		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
		if (!serialPort.isOpen()) {
			return false;
		}
		return true;
	}

	private boolean isSIK(SerialPort p) {
		byte[] setupMarker = "+++".getBytes();
		byte[] sikMarker = "ATI".getBytes();
		byte[] outMarker = "ATO".getBytes();
		p.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 1000);
		p.writeBytes(setupMarker, setupMarker.length);
		var buf = new MyByteArrayOutputStream();
		while (true) {
			int i;
			try {

				i = p.getInputStream().read(); // timeout not working because the timeout should be on the read and
												// write, this is not SIK specific but java specific ask Luc

				if (i == -1) {
					buf.close();
					return false;
				}
				buf.write((byte) i);

				if (buf.endsBy("OK".getBytes())) {
					markerWrite(p, buf, sikMarker);

				} else if (buf.endsBy("SiK".getBytes())) {
					markerWrite(p, buf, outMarker);

				} else if (buf.endsBy("ATO".getBytes())) {
					buf.close();
					return true;
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	private void markerWrite(SerialPort p, MyByteArrayOutputStream buf, byte[] marker) {
		byte[] separator = System.getProperty("line.separator").getBytes();
		buf.reset();
		p.writeBytes(marker, marker.length);
		p.writeBytes(separator, separator.length);
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