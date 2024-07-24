package idawi.routing;

import java.util.List;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.messaging.Message;
import idawi.routing.ComponentMatcher.multicast;

public class BFSRouting extends RoutingService<BFSRoutingParms> {

	public BFSRouting(Component node) {
		super(node);
	}

	@Override
	public String getAlgoName() {
		return "BFS routing";
	}

	@Override
	public String getFriendlyName() {
		return "bfs";
	}

	@Override
	public void acceptImpl(Message msg, final BFSRoutingParms parms) {
		if (!component.alreadyReceivedMsgs.contains(msg.ID) && !component.alreadySentMsgs.contains(msg.ID)) {
			var to = ((multicast) msg.qAddr.targetedComponents).target;
			parms.paths = component.localView().g.bfs.from(component).predecessors.pathsTo(to);
			parms.paths.stream().map(p -> p.getFirst()).collect(Collectors.groupingBy(l -> l.src)).entrySet()
					.forEach(e -> {
						var t = e.getKey();

						if (parms.acceptTransport.test(t)) {
							var links = e.getValue();
							t.send(msg, links, this);
						}
					});
		}
	}

	@Override
	public List<BFSRoutingParms> dataSuggestions() {
		return null;
	}

	@Override
	public ComponentMatcher defaultMatcher(BFSRoutingParms parms) {
		return ComponentMatcher.multicast(parms.paths.stream().map(p -> p.getLast().dest.component).toList());
	}

}
