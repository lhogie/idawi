package idawi.routing;

import java.util.Collection;
import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Route;
import idawi.Service;
import idawi.TransportLayer;

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

	public abstract Collection<ComponentInfo> findRelaysToReach(TransportLayer protocol, Set<ComponentInfo> to);

	public abstract void feedWith(Route route);
}
