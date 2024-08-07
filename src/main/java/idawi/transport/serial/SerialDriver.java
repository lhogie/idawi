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
				openPorts();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}

	public SerialDevice getCorrespondingDevice(SerialPort port) {
		for (SerialDevice alreadyInDevice : devices) {
			if (alreadyInDevice.p.getDescriptivePortName().equalsIgnoreCase(port.getDescriptivePortName())) {
				return alreadyInDevice;
			}
		}

		return null;
	}

	public void openPorts() throws InterruptedException {

		while (true) {
			for (var serialPort : SerialPort.getCommPorts()) {

				if (!serialPort.isOpen() && getCorrespondingDevice(serialPort) == null) {
					if ((!serialPort.getDescriptivePortName().contains("Bluetooth"))
							&& (!serialPort.getDescriptivePortName().contains("S4"))) {
						serialPort.openPort();
						serialPort.setBaudRate(115200);
						serialPort.setFlowControl(
								SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);
						serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);

						var device = isSIK(serialPort) ? new SikDevice(serialPort) : new SerialDevice(serialPort);
						newThread(device);
						System.out.println("okay for fetch");
						devices.add(device);
					}
				}
			}

			Thread.sleep(1000);
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

	private void newThread(SerialDevice d) {
		Idawi.agenda.threadPool.submit(() -> {
			try {
				var buf = new MyByteArrayOutputStream();

				while (true) {
					int i = d.p.getInputStream().read();

					if (i == -1) {
						buf.close();
						return;
					}

					buf.write((byte) i);

					for (var m : d.markers) {
						if (buf.endsBy(m.marker())) {
							m.callBack(buf.toByteArray(), this);
							buf.reset();
						}
					}
				}

			} catch (IOException err) {
				System.err.println("I/O error reading stream");
			}
		});
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