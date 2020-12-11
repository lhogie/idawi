package idawi.service;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.PeerRegistry;
import idawi.Service;
import idawi.net.NetworkingService;
import toools.collections.Collections;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class RandomConnector extends Service {
	public RandomConnector(Component node) {
		super(node);
		newThread_loop_periodic(1000, () -> {
			PeerRegistry peersNotInNeighborhood = new PeerRegistry(
					Collections.difference(component.descriptorRegistry, component.lookupService(NetworkingService.class).neighbors()));
			ComponentInfo randomPeer = peersNotInNeighborhood.pickRandomPeer();
			node.lookupService(PingPong.class).ping(randomPeer, 1000);
		});

		registerOperation(null, (msg, returns) -> {
		});
	}

	@Override
	public String getFriendlyName() {
		return "random agent connection";
	}
}
