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
			// Component.componentsInThisJVM.values()
			// .forEach(c ->
			// c.lookup(RegistryService.class).lookupOperation(broadcastLocalInfo.class).f());
		});
	}

	private final Map<String, ComponentDescriptor> name2descriptor = new HashMap<>();

	public RegistryService(Component component) {
		super(component);
		registerOperation(new add());
		registerOperation(new addAll());
		registerOperation(new broadcastLocalInfo());
		registerOperation(new clear());
		registerOperation(new list());
		registerOperation(new local());
		registerOperation(new lookUp());
		registerOperation(new names());
		registerOperation(new remove());
		registerOperation(new size());
		registerOperation(new updateAll());
	}

	public class broadcastLocalInfo extends TypedOperation {

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}

		public void f() {
			RegistryService.this.exec(new To().o(RegistryService.add.class), null, component.descriptor());
		}
	}

	public class add extends TypedOperation {

		@Override
		public String getDescription() {
			return null;
		}

		public void f(ComponentDescriptor d) {
			var alreadyHere = name2descriptor.get(d.name);

			if (alreadyHere == null || d.isNewerThan(alreadyHere)) {
				name2descriptor.put(d.name, d);
			}
		}
	}

	public class addAll extends TypedOperation {
		@Override
		public String getDescription() {
			return null;
		}

		public void addAll(Collection<ComponentDescriptor> s) {
			s.forEach(i -> lookup(add.class).f(i));
		}
	}

	public class size extends TypedOperation {
		public int size() {
			return name2descriptor.size();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class names extends TypedOperation {
		public Set<String> names() {
			return name2descriptor.keySet();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class updateAll extends TypedOperation {
		public void f() {
			var to = new OperationAddress(new To(new HashSet<>(name2descriptor.values())), RegistryService.local.class);
			RegistryService.this.exec(to, createQueue(), null).returnQ.collect().resultMessages().contents()
					.forEach(d -> lookup(add.class).f((ComponentDescriptor) d));
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class lookUp extends TypedOperation {
		public ComponentDescriptor f(String name) {
			var d = name2descriptor.get(name);
			return d;
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class remove extends TypedOperation {
		public ComponentDescriptor f(String name) {
			return name2descriptor.remove(name);
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class clear extends TypedOperation {
		public void f() {
			name2descriptor.clear();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class list extends TypedOperation {
		public Set<ComponentDescriptor> list() {
			return new HashSet<>(name2descriptor.values());
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class local extends TypedOperation {
		public ComponentDescriptor local() {
			return component.descriptor();
		}

		@Override
		public String getDescription() {
			return null;
		}
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
	public String getFriendlyName() {
		return "component registry";
	}

	public ComponentDescriptor ensureExists(String name) {
		ComponentDescriptor info = name2descriptor.get(name);

		if (info == null) {
			name2descriptor.put(name, info = new ComponentDescriptor());
			info.name = name;
		}

		return info;
	}

	public Set<ComponentDescriptor> lookupByRegexp(String re) {
		var r = new HashSet<ComponentDescriptor>();

		for (var e : name2descriptor.values()) {
			if (e.name.matches(re)) {
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
							b.name = n;
						}
					}

					m.add(c, protocol, b);
				}
			}
		}

		return m;
	}
}
