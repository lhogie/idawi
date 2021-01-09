package idawi.map;

import idawi.Component;
import idawi.IdawiExposed;
import idawi.Route;
import idawi.RouteEntry;
import idawi.Service;

public class MapService extends Service {

	private final NetworkMap map = new NetworkMap();

	public MapService(Component component) {
		super(component);
	}

	@IdawiExposed
	public NetworkMap get() {
		return map;
	}

	@Override
	public String getFriendlyName() {
		return "graph map";
	}

	public void feedWith(Route route) {
		int len = route.size();

		for (int i = 0; i < len; ++i) {
			RouteEntry e = route.get(i);

			if (i > 0) {
				map.add(route.get(i - 1).component, route.get(i - 1).protocolName, e.component);
			}

			if (i < len - 1) {
				map.add(e.component, e.protocolName, route.get(i + 1).component);
			}
		}
	}
}
