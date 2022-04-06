package idawi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import idawi.map.NetworkMap;
import toools.collections.Collections;
import toools.thread.Threads;

public class RegistryService extends Service {
	public final static AtomicBoolean run = new AtomicBoolean(true);

	static {
		var r = new Random();

		new Thread(() -> {
			while (run.get()) {
				if (Component.componentsInThisJVM.isEmpty()) {
					Threads.sleep(0.1);
				} else {
					var from = Collections.pickRandomObject(Component.componentsInThisJVM.values(), r);
					from.operation(RegistryService.broadcastLocalDescriptor.class).f(1);
					Threads.sleep(10d / Component.componentsInThisJVM.size());
				}
			}
		}).start();
	}

	private final Map<String, ComponentDescriptor> name2descriptor = java.util.Collections
			.synchronizedMap(new HashMap<>());

	public RegistryService(Component component) {
		super(component);
		registerOperation(new add());
		registerOperation(new addAll());
		registerOperation(new broadcastLocalDescriptor());
		registerOperation(new broadcastFullDatabase());
		registerOperation(new clear());
		registerOperation(new list());
		registerOperation(new local());
		registerOperation(new lookUp());
		registerOperation(new names());
		registerOperation(new remove());
		registerOperation(new size());
		registerOperation(new updateAll());
	}

	public class broadcastLocalDescriptor extends TypedInnerOperation {
		@Override
		public String getDescription() {
			return "broadcast local info to all nodes closer that 10 hops";
		}

		public void f(int nbHops) {
			var to = new To(null, nbHops, 1).o(RegistryService.add.class);
			RegistryService.this.exec(to, null, new OperationParameterList(component.descriptor()));
			// System.out.println(component + " sent " + component.descriptor());
		}
	}

	public class broadcastFullDatabase extends TypedInnerOperation {
		@Override
		public String getDescription() {
			return "broadcast all the known descriptors";
		}

		public void f(int nbHops) {
			var to = new To(null, nbHops, 1).o(RegistryService.add.class);
			RegistryService.this.exec(to, null, new OperationParameterList(name2descriptor.values()));
		}
	}

	public class add extends TypedInnerOperation {
		@Override
		public String getDescription() {
			return null;
		}

		public void f(ComponentDescriptor d) {
			// System.out.println(component + " received " + d);
			var alreadyHere = name2descriptor.get(d.name);

			if (alreadyHere == null || d.isNewerThan(alreadyHere)) {
				name2descriptor.put(d.name, d);
			}
		}
	}

	public class addAll extends TypedInnerOperation {
		@Override
		public String getDescription() {
			return null;
		}

		public void addAll(Collection<ComponentDescriptor> s) {
			s.forEach(i -> lookup(add.class).f(i));
		}
	}

	public class size extends TypedInnerOperation {
		public int size() {
			return name2descriptor.size();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class names extends TypedInnerOperation {
		public Set<String> names() {
			return name2descriptor.keySet();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class updateAll extends TypedInnerOperation {
		public void f() {
			var to = new OperationAddress(new To(new HashSet<>(name2descriptor.values())), RegistryService.local.class);
			RegistryService.this.exec(to, createQueue(), null).returnQ.recv_sync().messages.resultMessages().contents()
					.forEach(d -> lookup(add.class).f((ComponentDescriptor) d));
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class lookUp extends TypedInnerOperation {
		public ComponentDescriptor f(String name) {
			var d = name2descriptor.get(name);
			return d;
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class remove extends TypedInnerOperation {
		public ComponentDescriptor f(String name) {
			return name2descriptor.remove(name);
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class clear extends TypedInnerOperation {
		public void f() {
			name2descriptor.clear();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class list extends TypedInnerOperation {
		public Set<ComponentDescriptor> list() {
			return new HashSet<>(name2descriptor.values());
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class local extends TypedInnerOperation {
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
