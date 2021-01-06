package idawi.routing;

import idawi.ComponentDescriptor;
import idawi.NetworkMap;
import idawi.RegistryService;
import idawi.Route;

public class FullRouteTable extends RoutingTable<Route> {

	public void add(ComponentDescriptor destination, Route route) {
		map.put(destination, route);
	}

	@Override
	public void discard(ComponentDescriptor c) {
		var i = map.entrySet().iterator();

		while (i.hasNext()) {
			var e = i.next();

			if (e.getKey().equals(c)) {
				i.remove();
			} else if (e.getValue().contains(c)) {
				if (e.getValue().size() == 1) {
					i.remove();
				} else {
					e.getValue().remove(c);
				}
			}
		}
	}

	@Override
	public NetworkMap map() {
		var m = new NetworkMap();

		for (var e : map.entrySet()) {
			var target = e.getKey();
			var route = e.getValue();
			m.add(route.get(route.size() - 1).component, target);

			for (int i = 1; i < route.size(); ++i) {
				m.add(route.get(i - 1).component, route.get(i).component);
			}
		}

		return m;
	}

	@Override
	public void feedWith(Route r, ComponentDescriptor me) {
	}

	@Override
	public void feedWith(ComponentDescriptor d, RegistryService registry) {
	}
}