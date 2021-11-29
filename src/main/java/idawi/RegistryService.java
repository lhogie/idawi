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
					.forEach(c -> c.lookupService(RegistryService.class).lookupOperation(broadcastLocalInfo.class).f());
		});
	}

	private final Map<String, ComponentDescriptor> name2descriptor = new HashMap<>();

	@IdawiOperation
	public  add add = new add();
	@IdawiOperation
	public  addAll addAll = new addAll();
	@IdawiOperation
	public  broadcastLocalInfo broadcastLocalInfo = new broadcastLocalInfo();
	@IdawiOperation
	public  clear clear = new clear();
	@IdawiOperation
	public  list list = new list();
	@IdawiOperation
	public  local local = new local();
	@IdawiOperation
	public  lookUp lookUp = new lookUp();
	@IdawiOperation
	public  names names = new names();
	@IdawiOperation
	public  remove remove = new remove();
	@IdawiOperation
	public  size size = new size();
	@IdawiOperation
	public  updateAll updateAll = new updateAll();

	public RegistryService(Component component) {
		super(component);
	}

	public class broadcastLocalInfo extends InnerClassTypedOperation {

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}

		public void f() {
			start(new ComponentAddress((Set<ComponentDescriptor>) null).o(RegistryService.add.class), false,
					new OperationParameterList(component.descriptor()));
		}
	}

	public class add extends InnerClassTypedOperation {

		@Override
		public String getDescription() {
			return null;
		}

		public void f(ComponentDescriptor d) {
			var alreadyHere = name2descriptor.get(d.friendlyName);

			if (alreadyHere == null || d.isNewerThan(alreadyHere)) {
				name2descriptor.put(d.friendlyName, d);
			}
		}
	}

	public class addAll extends InnerClassTypedOperation {
		@Override
		public String getDescription() {
			return null;
		}

		public void addAll(Collection<ComponentDescriptor> s) {
			s.forEach(i -> lookupOperation(add.class).f(i));
		}
	}

	public class size extends InnerClassTypedOperation {
		public int size() {
			return name2descriptor.size();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class names extends InnerClassTypedOperation {
		public Set<String> names() {
			return name2descriptor.keySet();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class updateAll extends InnerClassTypedOperation {
		public void updateAll() {
			var to = new OperationAddress(new ComponentAddress(new HashSet<>(name2descriptor.values())),
					RegistryService.local.class);
			start(to, true, null).returnQ.collect().resultMessages().contents()
					.forEach(d -> lookupOperation(add.class).f((ComponentDescriptor) d));
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class lookUp extends InnerClassTypedOperation {
		public ComponentDescriptor lookup(String name) {
			return name2descriptor.get(name);
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class remove extends InnerClassTypedOperation {
		public ComponentDescriptor remove(String name) {
			return name2descriptor.remove(name);
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class clear extends InnerClassTypedOperation {
		public void clear() {
			name2descriptor.clear();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class list extends InnerClassTypedOperation {
		public Set<ComponentDescriptor> list() {
			return new HashSet<>(name2descriptor.values());
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class local extends InnerClassTypedOperation {
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
