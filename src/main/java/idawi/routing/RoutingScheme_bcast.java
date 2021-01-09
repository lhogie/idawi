package idawi.routing;

import java.util.Collection;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Route;
import idawi.map.NetworkMap;
import idawi.net.NetworkingService;
import idawi.net.TransportLayer;

public class RoutingScheme_bcast extends RoutingService {

	public RoutingScheme_bcast(Component node) {
		super(node);
	}

	@Override
	public Collection<ComponentDescriptor> findRelaysToReach(TransportLayer protocol, Set<ComponentDescriptor> to) {
		Collection<ComponentDescriptor> neighbors = protocol.neighbors();

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
	public String getAlgoName() {
		return "broadcast";
	}

	@Override
	public void feedWith(Route route) {
	}

	@Override
	public NetworkMap map() {
		var m = new NetworkMap();
		m.add(component.descriptor());
		component.lookupService(NetworkingService.class).neighbors().forEach(n -> m.add(n));
		return m;
	}

}
