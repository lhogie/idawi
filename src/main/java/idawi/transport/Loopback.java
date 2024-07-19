package idawi.transport;

import java.util.Collection;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;

public class Loopback extends TransportService {

	public Loopback(Component c) {
		super(c);
		c.localView().g.markLinkActive(this, this); // loopback
	}

	@Override
	public String getName() {
		return "loopback";
	}

	@Override
	public void dispose(Link l) {
//		l.activity.close();
	}

	@Override
	public double latency() {
		return 0;
	}

	@Override
	protected void multicast(byte[] msg, Collection<Link> outLinks) {
		var msgClone = (Message) serializer.fromBytes(msg);
		Idawi.agenda.scheduleNow(() -> processIncomingMessage(msgClone));
	}

}
