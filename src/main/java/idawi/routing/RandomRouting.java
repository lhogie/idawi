package idawi.routing;

import java.util.Collection;
import java.util.Random;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.NetworkMap;
import idawi.Route;
import idawi.net.NetworkingService;
import idawi.net.TransportLayer;
import toools.collections.Collections;

public class RandomRouting extends RoutingService {

	private final static Random r = new Random();

	public RandomRouting(Component component) {
		super(component);
	}

	@Override
	public Collection<ComponentDescriptor> findRelaysToReach(TransportLayer protocol, Set<ComponentDescriptor> to) {
		return Set.of(Collections.pickRandomObject(protocol.neighbors(), r));
	}

	@Override
	public String getAlgoName() {
		return "random";
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
