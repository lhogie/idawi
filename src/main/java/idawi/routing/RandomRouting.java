package idawi.routing;

import java.util.Collection;
import java.util.Random;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Route;
import idawi.To;
import idawi.map.NetworkMap;
import idawi.net.NetworkingService;
import idawi.net.TransportLayer;
import toools.collections.Collections;

public class RandomRouting extends RoutingService {

	private final static Random r = new Random();

	public RandomRouting(Component component) {
		super(component);
	}

	@Override
	public Collection<ComponentDescriptor> relaysTo(Set<ComponentDescriptor> to, TransportLayer protocol) {
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
		component.lookup(NetworkingService.class).neighbors().forEach(n -> m.add(n));
		return m;
	}

	@Override
	public To decode(String remove) {
		return null;
	}

}
