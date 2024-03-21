package idawi.transport;

import java.util.Collection;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import toools.math.MathsUtilities;

public class SharedMemoryTransport extends TransportService {

	public SharedMemoryTransport(Component c) {
		super(c);
	}

	@Override
	public String getName() {
		return "shared mem";
	}

	@Override
	public void dispose(Link l) {
//		l.activity.close();
	}

	@Override
	public double latency() {
		return MathsUtilities.pickRandomBetween(0.000000002, 0.000000009, Idawi.prng);
	}

	@Override
	protected void multicast(byte[] msg, Collection<Link> outLinks) {
		var msgClone = (Message) serializer.fromBytes(msg);

		for (var l : outLinks) {
			Idawi.agenda.scheduleNow(() -> l.dest.processIncomingMessage(msgClone));
		}
	}

	@Override
	protected void bcast(byte[] msg) {
		multicast(msg, activeOutLinks());
	}

}
