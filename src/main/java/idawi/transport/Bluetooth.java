package idawi.transport;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import toools.exceptions.NotYetImplementedException;
import toools.math.MathsUtilities;

public class Bluetooth extends WirelessTransport {

	private static final Object lock = new Object();

	public static void main(String[] args) {
		System.out.println(System.getProperties());
		try {
			// 1
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			System.out.println(localDevice.getFriendlyName());

			// 2
			DiscoveryAgent agent = localDevice.getDiscoveryAgent();

			// 3
			agent.startInquiry(DiscoveryAgent.GIAC, new MyDiscoveryListener());

			try {
				synchronized (lock) {
					lock.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Device Inquiry Completed. ");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class MyDiscoveryListener implements DiscoveryListener {

		@Override
		public void deviceDiscovered(RemoteDevice btDevice, DeviceClass arg1) {
			String name;
			try {
				name = btDevice.getFriendlyName(false);
			} catch (Exception e) {
				name = btDevice.getBluetoothAddress();
			}

			System.out.println("device found: " + name);

		}

		@Override
		public void inquiryCompleted(int arg0) {
			synchronized (lock) {
				lock.notify();
			}
		}

		@Override
		public void serviceSearchCompleted(int arg0, int arg1) {
		}

		@Override
		public void servicesDiscovered(int arg0, ServiceRecord[] arg1) {
		}

	}

	public Bluetooth(Component c) {
		super(c);
	}

	@Override
	public double typicalEmissionRange() {
		return 10;
	}

	@Override
	public String getName() {
		return "bluetooth";
	}


	@Override
	protected void sendImpl(Message msg) {
		throw new NotYetImplementedException();
	}

	@Override
	public void dispose(Link l) {
	}

	@Override
	public double latency() {
		return MathsUtilities.pickRandomBetween(0.034, 0.200, Idawi.prng);
	}
}
