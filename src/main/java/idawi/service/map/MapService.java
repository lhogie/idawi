package idawi.service.map;

import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.NeighborhoodListener;
import idawi.Service;
import idawi.To;
import idawi.TransportLayer;
import idawi.net.NetworkingService;
import toools.collections.Collections;

public class MapService extends Service {

	public NetworkMap localMap = new NetworkMap();

	public MapService(Component thing) {
		super(thing);
		localMap.add(thing.descriptor());

		newThread_loop(5000, () -> {
			To to = new To();
			to.service = id;
			to.operationOrQueue = "neighborhood";
			send(thing.lookupService(NetworkingService.class).neighbors(), to, null);
		});

		registerOperation("neighborhood", (msg, returns) -> {
			ComponentInfo src = msg.route.source().component;
			Set<ComponentInfo> updatedNeighborhood = (Set<ComponentInfo>) msg.content;
			// System.out.println(msg);
			Set<ComponentInfo> formerNeighborhood = localMap.get(src);

			if (formerNeighborhood == null) {
				updatedNeighborhood.forEach(n -> localMap.add(src, n));
			} else {
				Collections.difference(updatedNeighborhood, formerNeighborhood).forEach(n -> localMap.add(src, n));

				Collections.difference(formerNeighborhood, updatedNeighborhood).forEach(n -> localMap.remove(src, n));
			}
		});

		registerOperation("new neighbor", (msg, returns) -> {
			ComponentInfo newNeighbor = ((PeerJoinedEvent) msg.content).peer;
			localMap.add(msg.route.source().component, newNeighbor);
		});

		registerOperation("get_map", (msg, returns) -> returns.accept(localMap));

		registerOperation("neighbor left", (msg, returns) -> {
			ComponentInfo oldNeighbor = ((PeerLeftEvent) msg.content).peer;
			localMap.remove(msg.route.source().component, oldNeighbor);
		});

		thing.lookupService(NetworkingService.class).transport.listeners.add(localNeighborhoodListener);
	}

	@Override
	public String getFriendlyName() {
		return "construct a map of the network";
	}

	private NeighborhoodListener localNeighborhoodListener = new NeighborhoodListener() {

		@Override
		public void peerJoined(ComponentInfo newPeer, TransportLayer protocol) {
			localMap.add(component.descriptor(), newPeer);

			To to = new To();
			to.service = id;
			to.operationOrQueue = "new neighbor";
			send(new PeerJoinedEvent(newPeer, protocol.getName()), to, null);
		}

		@Override
		public void peerLeft(ComponentInfo leftPeer, TransportLayer protcol) {
			localMap.remove(component.descriptor(), leftPeer);

			To to = new To();
			to.service = id;
			to.operationOrQueue = "neighbor left";
			send(new PeerLeftEvent(leftPeer, protcol.getName()), to, null);
		}
	};
}
