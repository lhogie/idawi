package idawi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import idawi.map.NetworkMap;
import toools.thread.Threads;

public class RegistryService extends Service {

	static {
		Threads.newThread_loop(1000, () -> true, () -> {
			Component.componentsInThisJVM.values()
					.forEach(c -> c.lookupService(RegistryService.class).broadcastLocalInfo());
		});
	}

	private final Map<String, ComponentDescriptor> name2descriptor = new HashMap<>();

	public RegistryService(Component component) {
		super(component);
	}

	@IdawiExposed
	public void broadcastLocalInfo() {
		send(component.descriptor(), new To(RegistryService.class, "add"), null);
	}

	@IdawiExposed
	public void add(ComponentDescriptor d) {
		var alreadyHere = name2descriptor.get(d.friendlyName);

		if (alreadyHere == null || d.isNewerThan(alreadyHere)) {
			name2descriptor.put(d.friendlyName, d);
		}
	}

	@IdawiExposed
	public void addAll(Collection<ComponentDescriptor> s) {
		s.forEach(i -> add(i));
	}

	@IdawiExposed
	public int size() {
		return name2descriptor.size();
	}

	@IdawiExposed
	public Set<String> names() {
		return name2descriptor.keySet();
	}

	@IdawiExposed
	public void updateAll() {
		call(new To(new HashSet<>(name2descriptor.values()), RegistryService.class, "local")).collect().resultMessages()
				.contents().forEach(d -> add((ComponentDescriptor) d));
	}

	@IdawiExposed
	public ComponentDescriptor lookup(String name) {
		return name2descriptor.get(name);
	}

	@IdawiExposed
	public ComponentDescriptor remove(String name) {
		return name2descriptor.remove(name);
	}

	@IdawiExposed
	public void clear() {
		name2descriptor.clear();
	}

	@IdawiExposed
	public Set<ComponentDescriptor> list() {
		return new HashSet<>(name2descriptor.values());
	}

	@IdawiExposed
	public ComponentDescriptor local() {
		return component.descriptor();
	}

	@IdawiExposed
	public ComponentDescriptor pickRandomPeer() {
		if (name2descriptor.isEmpty()) {
			return null;
		} else {
			var l = new ArrayList<>(name2descriptor.values());
			return l.get(ThreadLocalRandom.current().nextInt(l.size()));
		}
	}

	@Override
	public String getFriendlyName() {
		return "component registry";
	}

	public ComponentDescriptor ensureExists(String name) {
		ComponentDescriptor info = name2descriptor.get(name);

		if (info == null) {
			name2descriptor.put(name, info = new ComponentDescriptor());
			info.friendlyName = name;
		}

		return info;
	}

	@IdawiExposed
	public Set<ComponentDescriptor> lookupByRegexp(String re) {
		var r = new HashSet<ComponentDescriptor>();

		for (var e : name2descriptor.values()) {
			if (e.friendlyName.matches(re)) {
				r.add(e);
			}
		}

		return r;
	}

	public NetworkMap map() {
		var m = new NetworkMap();
		Map<String, ComponentDescriptor> missing = new HashMap<>();

		for (var c : name2descriptor.values()) {
			for (var e : c.protocol2neighbors.entrySet()) {
				String protocol = e.getKey();

				for (var n : e.getValue()) {
					ComponentDescriptor b = name2descriptor.get(n);

					if (b == null) {
						b = missing.get(n);

						if (b == null) {
							b = missing.put(n, b = new ComponentDescriptor());
							b.friendlyName = n;
						}
					}

					m.add(c, protocol, b);
				}
			}
		}

		return m;
	}
}
