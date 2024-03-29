package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.TransportService;

public class BlindBroadcasting extends RoutingService<RoutingParameters> {

	// public final BloomFilterForLong alreadyReceivedMsgs2 = new
	// BloomFilterForLong(1000);

	public BlindBroadcasting(Component node) {
		super(node);
	}

	@Override
	public String getAlgoName() {
		return "blind broadcasting";
	}

	@Override
	synchronized public void acceptImpl(Message msg, RoutingParameters parms) {
		// the message was never received
		if (!component.alreadyReceivedMsgs.contains(msg.ID) && !component.alreadySentMsgs.contains(msg.ID)) {
			for (var t : component.services(TransportService.class)) {
				t.send(msg, null, this, parms);
			}

			listeners.forEach(l -> l.messageForwarded(this, msg));
		} else {
			listeners.forEach(l -> l.messageDropped(this, msg));
		}
	}

	@Override
	public List<RoutingParameters> dataSuggestions() {
		var l = new ArrayList<RoutingParameters>();
		l.add(new EmptyRoutingParms());
		return l;
	}

	@Override
	public ComponentMatcher defaultMatcher(RoutingParameters parms) {
		return ComponentMatcher.all;
	}
}
