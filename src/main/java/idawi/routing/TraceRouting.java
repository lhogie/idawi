package idawi.routing;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.IdawiExposed;
import idawi.Route;
import idawi.net.TransportLayer;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class TraceRouting extends RoutingTableBasedRouting<FullRouteTable> {
	public FullRouteTable routingTable = new FullRouteTable();

	public TraceRouting(Component node) {
		super(node);
	}

	@Override
	public Collection<ComponentDescriptor> findRelaysToReach(TransportLayer protocol, Set<ComponentDescriptor> to) {
		return null;}/*
		Collection<ComponentDescriptor> neighbors = protocol.neighbors();

		// if it's a broadcast message
		if (to == null) {
			return neighbors;
		}
		// the msg is targeted to specific nodes
		else {
			// but the node is disconnected
			if (neighbors.isEmpty()) {
				// try to reach the destinations directly
				return to;
			} else {
				Set<ComponentDescriptor> relays = new HashSet<>(to.size());

				for (ComponentDescriptor t : to) {
					Route relay = routingTable.get(t);

					// if one relay can't be found, broadcast
					if (relay == null) {
						return neighbors;
					}

					relays.add(relay);
				}

				return relays;
			}
		}
	}*/

	@Override
	@IdawiExposed
	public String getAlgoName() {
		return "default";
	}
}
