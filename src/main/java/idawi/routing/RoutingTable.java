package idawi.routing;

import java.util.HashMap;

import idawi.ComponentInfo;
import idawi.Route;

public class RoutingTable extends HashMap<ComponentInfo, ComponentInfo> {

	public void feedWith(Route r) {
		ComponentInfo relay = r.last().component;
		int len = r.size();

		for (int i = 0; i < len - 1; ++i) {
			ComponentInfo p = r.get(i).component;
			put(p, relay);
		}
	}
}
