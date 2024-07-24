package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.TransportService;
import toools.io.Cout;

public class FloodingWithSelfPruning extends RoutingService<FloodingWithSelfPrunningData> {

	public FloodingWithSelfPruning(Component node) {
		super(node);
	}

	@Override
	public String getAlgoName() {
		return "Flooding With Self Pruning";
	}

	@Override
	public String getFriendlyName() {
		return "fwsp";
	}

	@Override
	public void acceptImpl(Message msg, FloodingWithSelfPrunningData p) {
		if (!component.alreadyReceivedMsgs.contains(msg.ID) && !component.alreadySentMsgs.contains(msg.ID)) {
			var myNeighbors = new ArrayList<>(component.outLinks().stream().map(n -> n.dest.component).toList());

			if (p == null || msg.route.isEmpty()) {
				p = new FloodingWithSelfPrunningData();
				p.outNeighbors = myNeighbors;

				for (var t : component.services(TransportService.class)) {
					t.send(msg, null, this);
				}
			} else {
				myNeighbors.removeAll(convert(msg.route.getLast().routing.parms).outNeighbors);
				myNeighbors.remove(msg.sender());

				if (!myNeighbors.isEmpty()) {
					// finds the links to these neighbors
					var links = myNeighbors.stream()
							.flatMap(n -> component.localView().g.findLinksConnecting(component, n).stream()).toList();

					for (var t : component.services(TransportService.class)) {
						t.send(msg, links, this);
					}
				}
			}
		}
	}

	@Override
	public FloodingWithSelfPrunningData defaultData() {
		var p = new FloodingWithSelfPrunningData();
		p.outNeighbors = component.outLinks().stream().map(i -> i.dest.component).toList();
		return p;
	}

	@Override
	public List<FloodingWithSelfPrunningData> dataSuggestions() {
		var l = new ArrayList<FloodingWithSelfPrunningData>();
		l.add(new FloodingWithSelfPrunningData());
		return l;
	}

	@Override
	public ComponentMatcher defaultMatcher(FloodingWithSelfPrunningData parms) {
		return ComponentMatcher.all;
	}

}
