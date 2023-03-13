package idawi.routing;

import java.util.HashSet;

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
	public BFSRoutingParms decode(String s) {
		BFSRoutingParms to = new BFSRoutingParms();
		to.recipients = new HashSet<>();

		for (var n : s.split(" *, *")) {
			var c = component.mapService().map.lookup(n);

			if (c != null) {
				to.recipients.add(c);
			}
		}

		return to;
	}

	@Override
	public void accept(Message msg, BFSRoutingParms parms) {
		var p = convert(parms);

		if (!alreadyReceivedMsgs.contains(msg.ID)) {
			alreadyReceivedMsgs.add(msg.ID);
			var recipients = p.recipients;
			var relays = component.mapService().bfsResult.get().predecessors.successors(component.ref(), recipients);

			for (var t : component.services(TransportService.class)) {
				t.multicast(msg, relays, this, parms);
			}
		}
	}

	@Override
	public BFSRoutingParms createDefaultRoutingParms() {
		return new BFSRoutingParms();
	}

	@Override
	public TargetComponents naturalTarget(BFSRoutingParms p) {
		return c -> p.recipients.contains(c);
	}

}
