package idawi.service.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
	public ComponentInfo local() {
		return component.descriptor();
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

	public Collection<ComponentInfo> descriptors() {
		return name2info.values();
	}
}
