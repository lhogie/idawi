package idawi.routing;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.RegistryService;
import idawi.net.TransportLayer;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class RoutingScheme1 extends RoutingTableBasedRouting<One2OneRoutingTable> {

	public RoutingScheme1(Component node) {
		super(node);
	}

	@Override
	public Collection<ComponentDescriptor> findRelaysToReach(TransportLayer protocol, Set<ComponentDescriptor> to) {
		Collection<ComponentDescriptor> neighbors = protocol.neighbors();

		// if it's a broadcast message
		if (to == null) {
			// but the node is disconnected
			if (neighbors.isEmpty()) {
				return lookupOperation(RegistryService.list.class).list();
			} else {
				return neighbors;
			}
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
					ComponentDescriptor relay = routingTable.get(t);

					// if one relay can't be found, broadcast
					if (relay == null) {
						return neighbors;
					}

					relays.add(relay);
				}

				return relays;
			}
		}
	}

	@Override
	public String getAlgoName() {
		return "default";
	}

}
