package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import idawi.transport.TransportService;

public class ProbabilisticBroadcasting extends RoutingService<ProbabilisticBroadcastingParms> {

	// public final BloomFilterForLong alreadyReceivedMsgs2 = new
	// BloomFilterForLong(1000);

	public ProbabilisticBroadcasting(Component node) {
		super(node);
	}

	@Override
	public String getAlgoName() {
		return "probabilistic broadcasting";
	}

	@Override
	synchronized public void acceptImpl(Message msg, ProbabilisticBroadcastingParms parms) {
		// the message was never received
		if (!component.alreadyReceivedMsgs.contains(msg.ID) && !component.alreadySentMsgs.contains(msg.ID)
				&& Idawi.prng.nextDouble() < parms.p) {
			for (var t : component.services(TransportService.class)) {
				if (parms.acceptTransport.test(t)) {

					t.send(msg, null, this);
				}
			}

			listeners.forEach(l -> l.messageForwarded(this, msg));
		} else {
			listeners.forEach(l -> l.messageDropped(this, msg));
		}
	}

	@Override
	public List<ProbabilisticBroadcastingParms> dataSuggestions() {
		var l = new ArrayList<ProbabilisticBroadcastingParms>();

		for (double p = 0; p <= 1; p += 0.2) {
			var parms = new ProbabilisticBroadcastingParms();
			parms.p = p;
			l.add(parms);
		}

		return l;
	}

	@Override
	public ComponentMatcher defaultMatcher(ProbabilisticBroadcastingParms parms) {
		return ComponentMatcher.all;
	}
}
