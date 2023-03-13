package idawi.routing;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class BlindBroadcasting extends RoutingService<RoutingParms> {
	public final LongSet alreadyReceivedMsgs = new LongOpenHashSet();

	public BlindBroadcasting(Component node) {
		super(node);
	}

	@Override
	public String getAlgoName() {
		return "blind broadcasting";
	}

	@Override
	public void accept(Message msg, RoutingParms parms) {
		// the message was never received
		if (!alreadyReceivedMsgs.contains(msg.ID)) {
			alreadyReceivedMsgs.add(msg.ID);
			component.services(TransportService.class).forEach(t -> t.bcast(msg, this, parms));
		}
	}

	@Override
	public RoutingParms createDefaultRoutingParms() {
		return new RoutingParms();
	}

	@Override
	public TargetComponents naturalTarget(RoutingParms parms) {
		return TargetComponents.all;
	}

	@Override
	public RoutingParms decode(String s) {
		if (!s.trim().isEmpty())
			throw new IllegalArgumentException(getAlgoName() + " accepts no parameters");

		return null;
	}
}
