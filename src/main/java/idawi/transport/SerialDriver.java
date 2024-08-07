package idawi.transport;

import idawi.Component;

public class SerialDriver extends TransportService implements Broadcastable {

	public SerialDriver(Component c) {
		super(c);

	}

	@Override
	public String getName() {
		return "serial ports";
	}

	@Override
	public void dispose(Link l) {
//		l.activity.close();
	}

	@Override
	public double latency() {
		return 0;
	}
}
