package idawi.routing;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import idawi.ComponentInfo;
import idawi.Route;
import idawi.TransportLayer;
import idawi.net.NetworkingService;

public class RoutingScheme_bcast extends RoutingScheme {

	public RoutingScheme_bcast(RoutingService s) {
		super(s);
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
	public void print(Consumer<String> out) {
		out.accept("all messages sent to neighbors: " + s.component.lookupService(NetworkingService.class).neighbors());
	}
}
