package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import toools.collections.Collections;

public class FloodingWithSelfPruning extends RoutingService<SPPParm> {

	public final LongSet alreadyReceivedMsgs = new LongOpenHashSet();

	public FloodingWithSelfPruning(Component node) {
		super(node);
	}

	@Override
	public String getAlgoName() {
		return "Flooding With Self Pruning";
	}
	@Override
	public String webShortcut() {
		return "fwsp";
	}


	@Override
	public void accept(Message msg, SPPParm p) {
		// the message was never received
		if (!alreadyReceivedMsgs.contains(msg.ID)) {
			alreadyReceivedMsgs.add(msg.ID);
			var myNeighbors = component.neighbors().stream().map(n -> n.dest.component).toList();
			var routingParms = convert(msg.currentRoutingParameters());
			var srcNeighbors = routingParms.neighbors;

			// if I have neighbors that the source doesn't know
			if (!Collections.difference(myNeighbors, srcNeighbors).isEmpty()) {
				component.services(TransportService.class).forEach(t -> t.bcast(msg, this, p));
			}
		}
	}

	@Override
	public SPPParm defaultData() {
		var p = new SPPParm();
		p.neighbors = component.neighbors().stream().map(i -> i.dest.component).toList();
		return p;
	}

	@Override
	public List<SPPParm> dataSuggestions() {
		var l = new ArrayList<SPPParm>();
		l.add(new SPPParm());
		return l;
	}

	@Override
	public TargetComponents naturalTarget(SPPParm parms) {
		return TargetComponents.all;
	}

}
