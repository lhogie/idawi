package idawi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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

	@ExposedOperation
	public void broadcastLocalInfo() {
		send(component.descriptor(), new To(RegistryService.class, "add"), null);
	}

	@ExposedOperation
	public void add(ComponentDescriptor d) {
		var alreadyHere = name2descriptor.get(d.friendlyName);

		if (alreadyHere == null || d.isNewerThan(alreadyHere)) {
			name2descriptor.put(d.friendlyName, d);
		}
	}

	@ExposedOperation
	public void addAll(Collection<ComponentDescriptor> s) {
		s.forEach(i -> add(i));
	}

	@ExposedOperation
	public int size() {
		return name2descriptor.size();
	}

	@ExposedOperation
	public Set<String> names() {
		return name2descriptor.keySet();
	}

	@ExposedOperation
	public void updateAll() {
		call(new To(new HashSet<>(name2descriptor.values()), RegistryService.class, "local")).collect().resultMessages()
				.contents().forEach(d -> add((ComponentDescriptor) d));
	}

	@ExposedOperation
	public ComponentDescriptor lookup(String name) {
		return name2descriptor.get(name);
	}

	@ExposedOperation
	public ComponentDescriptor remove(String name) {
		return name2descriptor.remove(name);
	}

	@ExposedOperation
	public void clear() {
		name2descriptor.clear();
	}

	@ExposedOperation
	public Set<ComponentDescriptor> list() {
		return new HashSet<>(name2descriptor.values());
	}

	@ExposedOperation
	public ComponentDescriptor local() {
		return component.descriptor();
	}

	@ExposedOperation
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


	@ExposedOperation
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

		for (var d : name2descriptor.values()) {
			for (var n : d.neighbors) {
				m.add(d, ensureExists(n));
			}
		}

		return m;
	}
}
