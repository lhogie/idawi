package idawi.routing;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class BFSRouting extends RoutingService<BFSRoutingParms> {

	public final LongSet alreadyReceivedMsgs = new LongOpenHashSet();

	public BFSRouting(Component node) {
		super(node);
	}

	@Override
	public String getAlgoName() {
		return "P2P routing";
	}

	@Override
	public long sizeOf() {
		return super.sizeOf() + alreadyReceivedMsgs.size() * 8 + 8;
	}

	@Override
	public void accept(Message msg, BFSRoutingParms parms) {
		var p = convert(parms);

		if (!alreadyReceivedMsgs.contains(msg.ID)) {
			alreadyReceivedMsgs.add(msg.ID);
			var recipients = p.recipients;
			var relays = component.digitalTwinService().bfsResult.get().predecessors.successors(component, recipients);

			for (var t : component.services(TransportService.class)) {
				t.multicast(msg, relays, this, parms);
			}
		}
	}

	@Override
	public BFSRoutingParms defaultData() {
		return new BFSRoutingParms();
	}

	@Override
	public TargetComponents naturalTarget(BFSRoutingParms p) {
		return c -> p.recipients.contains(c);
	}

}
