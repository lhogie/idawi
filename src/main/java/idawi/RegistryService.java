package idawi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import idawi.AsMethodOperation.OperationID;
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

	public static OperationID broadcastLocalInfo;

	@IdawiOperation
	public void broadcastLocalInfo() {
		trigger(new ServiceAddress((Set<ComponentDescriptor>) null, RegistryService.class), RegistryService.add, false,
				new OperationParameterList(component.descriptor()));
	}

	public static OperationID add;

	@IdawiOperation
	public void add(ComponentDescriptor d) {
		var alreadyHere = name2descriptor.get(d.friendlyName);

		if (alreadyHere == null || d.isNewerThan(alreadyHere)) {
			name2descriptor.put(d.friendlyName, d);
		}
	}

	public static OperationID addAll;

	@IdawiOperation
	public void addAll(Collection<ComponentDescriptor> s) {
		s.forEach(i -> add(i));
	}

	public static OperationID size;

	@IdawiOperation
	public int size() {
		return name2descriptor.size();
	}

	public static OperationID names;

	@IdawiOperation
	public Set<String> names() {
		return name2descriptor.keySet();
	}

	public static OperationID updateAll;

	@IdawiOperation
	public void updateAll() {
		var to = new ServiceAddress(new HashSet<>(name2descriptor.values()), RegistryService.class);
		trigger(to, RegistryService.local, true, null).returnQ.collect().resultMessages().contents()
				.forEach(d -> add((ComponentDescriptor) d));
	}

	public static OperationID lookUp;

	@IdawiOperation
	public ComponentDescriptor lookup(String name) {
		return name2descriptor.get(name);
	}

	public static OperationID remove;

	@IdawiOperation
	public ComponentDescriptor remove(String name) {
		return name2descriptor.remove(name);
	}

	public static OperationID clear;

	@IdawiOperation
	public void clear() {
		name2descriptor.clear();
	}

	public static OperationID list;

	@IdawiOperation
	public Set<ComponentDescriptor> list() {
		return new HashSet<>(name2descriptor.values());
	}

	public static OperationID local;

	@IdawiOperation
	public ComponentDescriptor local() {
		return component.descriptor();
	}

	public ComponentDescriptor pickRandomPeer() {
		if (name2descriptor.isEmpty()) {
			return null;
		} else {
			var l = new ArrayList<>(name2descriptor.values());
			return l.get(ThreadLocalRandom.current().nextInt(l.size()));
		}
	}

	@Override
	@IdawiOperation
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

	public Set<ComponentDescriptor> lookupByRegexp(String re) {
		var r = new HashSet<ComponentDescriptor>();

		for (var e : name2descriptor.values()) {
			if (e.friendlyName.matches(re)) {
				r.add(e);
			}
		}

		return r;
	}

	@IdawiOperation
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
