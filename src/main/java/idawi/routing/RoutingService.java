package idawi.routing;

import java.util.Collection;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Route;
import idawi.Service;
import idawi.map.NetworkMap;
import idawi.net.TransportLayer;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public abstract class RoutingService extends Service {

	public RoutingService(Component node) {
		super(node);
	}

	@Override
	public String getFriendlyName() {
		return "routing (" + getAlgoName() + ")";
	}

	public abstract String getAlgoName();

	public abstract Collection<ComponentDescriptor> findRelaysToReach(TransportLayer protocol,
			Set<ComponentDescriptor> to);

	public abstract void feedWith(Route route);
	
	public abstract NetworkMap map();
}
