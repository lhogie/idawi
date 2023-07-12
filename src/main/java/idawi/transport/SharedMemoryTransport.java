package idawi.transport;

import idawi.Component;
import idawi.RuntimeEngine;
import idawi.messaging.Message;

public class SharedMemoryTransport extends TransportService {

	public double emissionRange = Double.MAX_VALUE;

	public SharedMemoryTransport() {
	}

	public SharedMemoryTransport(Component c) {
		super(c);

		c.localView().links().add(new Link(this, this)); // loopback
	}

	@Override
	public String getName() {
		return "shared mem";
	}

	@Override
	public boolean canContact(Component c) {
		return c != null;
	}

	@Override
	protected void sendImpl(Message msg) {
		var c = msg.clone(component.ser);

		RuntimeEngine.threadPool.submit(() -> {
			try {
				c.route.last().link.dest.processIncomingMessage(c);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}



	@Override
	public void dispose(Link l) {
//		l.activity.close();
	}


}
