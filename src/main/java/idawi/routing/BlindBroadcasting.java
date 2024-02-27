package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.TransportService;

public class BlindBroadcasting extends RoutingService<RoutingData> {
	public final AutoForgettingLongList alreadyKnownMsgs = new AutoForgettingLongList(l -> l.size() < 1000);

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
	public long sizeOf() {
		return super.sizeOf() + alreadyKnownMsgs.sizeOf() + 8;
	}

	@Override
	synchronized public void accept(Message msg, RoutingData parms) {
		// the message was never received
		if (!alreadyKnownMsgs.contains(msg.ID)) {
			alreadyKnownMsgs.add(msg.ID);
			component.services(TransportService.class).forEach(t -> {
				t.multicast(msg, this, parms);
			});

			listeners.forEach(l -> l.messageForwarded(this, msg));
		} else {
			System.out.println(component + " dropped");
			listeners.forEach(l -> l.messageDropped(this, msg));
		}
	}

	@Override
	public List<RoutingData> dataSuggestions() {
		var l = new ArrayList<RoutingData>();
		l.add(new EmptyRoutingParms());
		return l;
	}

	@Override
	public ComponentMatcher naturalTarget(RoutingData parms) {
		return ComponentMatcher.all;
	}
}
