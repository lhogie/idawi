package idawi.routing;

import idawi.ComponentDescriptor;
import idawi.NetworkMap;
import idawi.RegistryService;
import idawi.Route;

public class One2OneRoutingTable extends RoutingTable<ComponentDescriptor> {

	@Override
	public void feedWith(Route r, ComponentDescriptor me) {
		int len = r.size();

		if (len > 1) {
			ComponentDescriptor relay = r.last().component;

			if (!relay.equals(me)) {
				for (int i = 0; i < len - 1; ++i) {
					ComponentDescriptor recipient = r.get(i).component;

					if (!recipient.equals(me)) {
						map.put(recipient, relay);
					}
				}
			}
		}
	}

	@Override
	public void feedWith(ComponentDescriptor d, RegistryService registry) {
		for (var n : d.neighbors) {
			ComponentDescriptor relay = registry.ensureExists(n);

			if (!relay.equals(registry.component.descriptor())) {
				map.put(d, relay);
			}
		}
	}

	@Override
	public void discard(ComponentDescriptor c) {
		var i = map.entrySet().iterator();

		while (i.hasNext()) {
			var e = i.next();

			if (e.getKey().equals(c)) {
				i.remove();
			} else if (e.getValue().equals(c)) {
				i.remove();
			}
		}
	}

	@Override
	public NetworkMap map() {
		var m = new NetworkMap();

		for (var e : map.entrySet()) {
			m.add(e.getKey(), e.getValue());
		}

		return m;
	}
}