package idawi.routing;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Operation;
import idawi.Route;
import idawi.TransportLayer;
import idawi.service.registry.RegistryService;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class RoutingScheme1 extends RoutingService {
	public RoutingScheme1(Component node) {
		super(node);
	}

	public static class RoutingTable extends HashMap<ComponentInfo, ComponentInfo> {
		public void feedWith(Route r, ComponentInfo me) {
			int len = r.size();

			if (len > 1) {
				ComponentInfo relay = r.last().component;

				for (int i = 0; i < len - 1; ++i) {
					ComponentInfo p = r.get(i).component;

					if (!p.equals(me)) {
						put(p, relay);
					}
				}
			}
		}
	}

	private RoutingTable routingTable = new RoutingTable();

	@Override
	public Collection<ComponentInfo> findRelaysToReach(TransportLayer protocol, Set<ComponentInfo> to) {
		Collection<ComponentInfo> neighbors = protocol.neighbors();

		// if it's a broadcast message
		if (to == null) {
			// but the node is disconnected
			if (neighbors.isEmpty()) {
				return service(RegistryService.class).list();
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
				Set<ComponentInfo> relays = new HashSet<>(to.size());

				for (ComponentInfo t : to) {
					ComponentInfo relay = routingTable.get(t);

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
	public void feedWith(Route route) {
		System.out.println(component + " rt: " + routingTable);
		System.out.println(component + " route: " + route);
		routingTable.feedWith(route, component.descriptor());
		System.out.println(component + " rt: " + routingTable);
	}

	@Override
	@Operation
	public String getAlgoName() {
		return "default";
	}

	@Operation
	public RoutingTable getRoutingTable() {
		return routingTable;
	}
}
