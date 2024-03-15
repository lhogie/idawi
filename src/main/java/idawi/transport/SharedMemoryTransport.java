package idawi.transport;

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
	protected void sendImpl(Message msg) {
		var msgClone = msg.clone(component.secureSerializer);

		Idawi.agenda.scheduleNow(() -> msgClone.route.last().link.dest.processIncomingMessage(msgClone));
	}

	@Override
	public void dispose(Link l) {
//		l.activity.close();
	}
	
	@Override
	public double latency() {
		return MathsUtilities.pickRandomBetween(0.000000002, 0.000000009, Idawi.prng);
	}


}
