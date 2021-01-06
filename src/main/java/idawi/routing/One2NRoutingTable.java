package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.ComponentDescriptor;
import idawi.NetworkMap;
import idawi.RegistryService;
import idawi.Route;

public class One2NRoutingTable extends RoutingTable<List<ComponentDescriptor>> {

	@Override
	public void feedWith(Route r, ComponentDescriptor me) {
		int len = r.size();

		if (len > 1) {
			ComponentDescriptor relay = r.last().component;

			if (!relay.equals(me)) {
				for (int i = 0; i < len - 1; ++i) {
					ComponentDescriptor recipient = r.get(i).component;

					if (!recipient.equals(me)) {
						add(recipient, relay);
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
				add(d, relay);
			}
		}
	}

	private void add(ComponentDescriptor p, ComponentDescriptor relay) {
		List<ComponentDescriptor> l = map.get(p);

		if (l == null) {
			map.put(p, l = new ArrayList<>());
			l.add(relay);
		} else {
			if (!l.get(0).equals(relay)) {
				l.remove(relay);
				l.add(0, relay);
			}
		}
	}

	public void retainFirst(int n) {
		if (n <= 0)
			throw new IllegalArgumentException();

		map.values().forEach(l -> {
			while (l.size() > n) {
				l.remove(l.size() - 1);
			}
		});
	}

	public int countEntries() {
		int n = 0;

		for (var l : map.values()) {
			n += l.size();
		}

		return n;
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
			for (var n : e.getValue()) {
				m.add(e.getKey(), n);
			}
		}

		return m;
	}
}