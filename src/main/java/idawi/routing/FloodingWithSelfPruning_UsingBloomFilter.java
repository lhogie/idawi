package idawi.routing;

import java.util.ArrayList;
import java.util.List;

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
	public long sizeOf() {
		return 8 + alreadyReceivedMsgs.size() * 8;
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
			var myNeighbors = component.outLinks();
			var routingParms = convert(msg.route.last().routing.parms);

			// if I have neighbors that the source doesn't know
			for (var n : myNeighbors) {
				if (!routingParms.neighbors.mightContain(n.dest.component.longHash())) {
					component.services(TransportService.class).forEach(t -> t.multicast(msg, this, parms));
					break;
				}
			}
		}
	}

	@Override
	public FloodingWithSelfPruning_UsingBloomFilterParm defaultData() {
		var neighbors = component.outLinks();
		var p = new FloodingWithSelfPruning_UsingBloomFilterParm(bloomSize(neighbors.size()));

		for (var n : neighbors) {
			p.neighbors.put(n.dest.component.longHash());
		}

		return p;
	}
	@Override
	public String webShortcut() {
		return "fwsp_bf";
	}


	@Override
	public List<FloodingWithSelfPruning_UsingBloomFilterParm> dataSuggestions() {
		var l = new ArrayList<FloodingWithSelfPruning_UsingBloomFilterParm>();
		l.add(new FloodingWithSelfPruning_UsingBloomFilterParm(1));
		return l;
	}

	protected int bloomSize(int size) {
		return component.outLinks().size();
	}

	@Override
	public ComponentMatcher naturalTarget(FloodingWithSelfPruning_UsingBloomFilterParm parms) {
		return ComponentMatcher.all;
	}

}
