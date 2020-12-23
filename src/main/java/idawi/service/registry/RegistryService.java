package idawi.service.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Operation;
import idawi.Route;
import idawi.RouteEntry;
import idawi.Service;

public class RegistryService extends Service {
	private final Map<String, ComponentInfo> name2info = new HashMap<>();

	public RegistryService(Component component) {
		super(component);
	}

	@Operation
	public void add(ComponentInfo i) {
		name2info.put(i.friendlyName, i);
	}

	@Operation
	public void addAll(Collection<ComponentInfo> s) {
		s.forEach(i -> add(i));
	}

	@Operation
	public int size() {
		return name2info.size();
	}

	@Operation
	public Set<String> names() {
		return name2info.keySet();
	}

	@Operation
	public ComponentInfo lookup(String name) {
		return name2info.get(name);
	}

	@Operation
	public ComponentInfo remove(String name) {
		return name2info.remove(name);
	}

	@Operation
	public void clear() {
		name2info.clear();
	}

	@Operation
	public Set<ComponentInfo> list() {
		return new HashSet<>(name2info.values());
	}

	@Operation
	public ComponentInfo local() {
		return component.descriptor();
	}

	@Operation
	public ComponentInfo pickRandomPeer() {
		if (size() == 0) {
			return null;
		} else {
			var l = new ArrayList<>(name2info.values());
			return l.get(ThreadLocalRandom.current().nextInt(l.size()));
		}
	}

	@Override
	public String getFriendlyName() {
		return "component registry";
	}

	private ComponentInfo ensureExists(String name) {
		ComponentInfo info = name2info.get(name);

		if (info == null) {
			name2info.put(name, info = new ComponentInfo());
		}

		return info;
	}

	public void feedWith(Route route) {
		int len = route.size();

		for (int i = 0; i < len; ++i) {
			RouteEntry e = route.get(i);
			ComponentInfo info = ensureExists(e.component.friendlyName);

			if (i > 0) {
				ensureNeighbors(info, route.get(i - 1).component.friendlyName);
			}

			if (i < len - 1) {
				ensureNeighbors(info, route.get(i + 1).component.friendlyName);
			}
		}
	}

	private void ensureNeighbors(ComponentInfo info, String neighbor) {
		if (!info.neighbors.contains(neighbor)) {
			info.neighbors.add(neighbor);
		}
	}

	public void update(ComponentInfo c) {
		name2info.put(c.friendlyName, c);
	}

	@Operation
	public Collection<ComponentInfo> descriptors() {
		return name2info.values();
	}

	@Operation
	public Set<ComponentInfo> lookupByRegexp(String re) {
		var r = new HashSet<ComponentInfo>();

		for (var e : name2info.values()) {
			if (e.friendlyName.matches(re)) {
				r.add(e);
			}
		}

		return r;
	}

}
