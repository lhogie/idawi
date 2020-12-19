package idawi.routing;

import java.util.Collection;
import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Route;
import idawi.TransportLayer;

public class RoutingScheme_bcast extends RoutingService {

	public RoutingScheme_bcast(Component node) {
		super(node);
	}

	@Override
	public Collection<ComponentInfo> findRelaysToReach(TransportLayer protocol, Set<ComponentInfo> to) {
		Collection<ComponentInfo> neighbors = protocol.neighbors();

		// if this is a bcast message
		if (to == null) {
			return neighbors;
		}

		if (neighbors.isEmpty()) {
			return to;
		}

		return neighbors;
	}

	@Override
	public void feedWith(Route route) {
	}

	@Override
	public String getAlgoName() {
		return "broadcast";
	}
}
