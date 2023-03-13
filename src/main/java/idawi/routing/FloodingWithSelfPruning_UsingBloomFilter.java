package idawi.routing;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class FloodingWithSelfPruning_UsingBloomFilter
		extends RoutingService<FloodingWithSelfPruning_UsingBloomFilterParm> {

	public final LongSet alreadyReceivedMsgs = new LongOpenHashSet();

	public FloodingWithSelfPruning_UsingBloomFilter(Component node) {
		super(node);
	}

	@Override
	public String getAlgoName() {
		return "Flooding With Self Pruning";
	}

	@Override
	public void accept(Message msg, FloodingWithSelfPruning_UsingBloomFilterParm parms) {
		// the message was never received
		if (!alreadyReceivedMsgs.contains(msg.ID)) {
			alreadyReceivedMsgs.add(msg.ID);
			var myNeighbors = component.mapService().map.outNeighbors(component.ref());
			var routingParms = convert(msg.currentRoutingParameters());

			// if I have neighbors that the source doesn't know
			for (var n : myNeighbors) {
				if (!routingParms.neighbors.mightContain(n.longHash())) {
					component.services(TransportService.class).forEach(t -> t.bcast(msg, this, parms));
					break;
				}
			}
		}
	}

	@Override
	public FloodingWithSelfPruning_UsingBloomFilterParm createDefaultRoutingParms() {
		var neighbors = component.mapService().map.outNeighbors(component.ref());
		var p = new FloodingWithSelfPruning_UsingBloomFilterParm(bloomSize(neighbors.size()));

		for (var n : neighbors) {
			p.neighbors.put(n.longHash());
		}

		return p;
	}

	protected int bloomSize(int size) {
		return component.mapService().map.outNeighbors(component.ref()).size();
	}

	@Override
	public TargetComponents naturalTarget(FloodingWithSelfPruning_UsingBloomFilterParm parms) {
		return TargetComponents.all;
	}

	@Override
	public FloodingWithSelfPruning_UsingBloomFilterParm decode(String s) {
		if (!s.trim().isEmpty())
			throw new IllegalArgumentException(getAlgoName() + " accepts no parameters");

		return null;
	}
}
