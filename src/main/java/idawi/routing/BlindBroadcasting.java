package idawi.routing;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class BlindBroadcasting extends RoutingService<RoutingData> {
	public final LongSet alreadyReceivedMsgs = new LongOpenHashSet();

	public BlindBroadcasting(Component node) {
		super(node);
	}

	@Override
	public String getAlgoName() {
		return "blind broadcasting";
	}

	@Override
	public long sizeOf() {
		return super.sizeOf() + alreadyReceivedMsgs.size() * 8 + 8;
	}
	@Override
	public void accept(Message msg, RoutingData parms) {
		// the message was never received
		if (!alreadyReceivedMsgs.contains(msg.ID)) {
			alreadyReceivedMsgs.add(msg.ID);
			component.services(TransportService.class).forEach(t -> t.bcast(msg, this, parms));
		}
	}

	@Override
	public RoutingData createDefaultRoutingParms() {
		return new EmptyRoutingParms();
	}

	@Override
	public TargetComponents naturalTarget(RoutingData parms) {
		return TargetComponents.all;
	}
}
