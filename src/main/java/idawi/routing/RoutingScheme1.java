package idawi.routing;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Operation;
import idawi.Route;
import idawi.TransportLayer;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class RoutingScheme1 extends RoutingService {
	public RoutingScheme1(Component node) {
		super(node);
		// TODO Auto-generated constructor stub
	}

	public static class RoutingTable extends HashMap<ComponentInfo, ComponentInfo> {
		public void feedWith(Route r) {
			ComponentInfo relay = r.last().component;
			int len = r.size();

			for (int i = 0; i < len - 1; ++i) {
				ComponentInfo p = r.get(i).component;
				put(p, relay);
			}
		}
	}
	
	private RoutingTable routingTable = new RoutingTable();

	@Override
	public Collection<ComponentInfo> findRelaysToReach(TransportLayer protocol, Set<ComponentInfo> to) {
		Collection<ComponentInfo> currentNeighbors = protocol.neighbors();

		// the msg is targeted to anyone
		if (to == null) {
			// but the node is disconnected
			if (currentNeighbors.isEmpty()) {
				return component.descriptorRegistry;
			} else {

				return currentNeighbors;
			}
		}
		// the msg is targeted to specific nodes
		else {
			// but the node is disconnected
			if (currentNeighbors.isEmpty()) {
				// try to reach the destinations directly
				return to;
			} else {
				Set<ComponentInfo> relays = new HashSet<>(to.size());

				for (ComponentInfo t : to) {
					ComponentInfo relay = routingTable.get(t);

					// if one relay can't be found, broadcast
					if (relay == null) {
						return currentNeighbors;
					}

					relays.add(relay);
				}

				return relays;
			}
		}
	}

	@Override
	public void feedWith(Route route) {
		routingTable.feedWith(route);
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
