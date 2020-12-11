package idawi.routing;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import idawi.ComponentInfo;
import idawi.Route;
import idawi.TransportLayer;
import toools.io.Cout;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class RoutingScheme1 extends RoutingScheme {

	public final RoutingTable routingTable = new RoutingTable();
	

	public RoutingScheme1(RoutingService s) {
		super(s);
	}

	@Override
	public Collection<ComponentInfo> findRelaysToReach(TransportLayer protocol, Set<ComponentInfo> to) {
		Collection<ComponentInfo> currentNeighbors = protocol.neighbors();

		// the msg is targeted to anyone
		if (to == null) {
			// but the node is disconnected
			if (currentNeighbors.isEmpty()) {
				return s.component.descriptorRegistry;
			}
			else {

				return currentNeighbors;
			}
		}
		// the msg is targeted to specific nodes
		else {
			// but the node is disconnected
			if (currentNeighbors.isEmpty()) {
				// try to reach the destinations directly
				return to;
			}
			else {
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
	public void print(Consumer<String> out) {
		routingTable.forEach((k, v) -> out.accept(k + " -> " + v));
	}
}
