package idawi.transport.serial;

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
		for (var d : devices) {
			if (d.getName().equalsIgnoreCase(port.getDescriptivePortName())) {
				return d;
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
						device.newThread(this);
						System.out.println("okay for fetch");
						devices.add(device);
					}
				}
			}

			Thread.sleep(1000);
		}
	}

	private boolean isSIK(SerialPort p) {
		// TODO Auto-generated method stub
		return false;
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