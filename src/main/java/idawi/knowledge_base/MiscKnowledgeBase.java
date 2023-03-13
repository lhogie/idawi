package idawi.knowledge_base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import idawi.Component;
import idawi.TypedInnerClassOperation;
import idawi.service.SystemMonitor;
import idawi.service.time.TimeService;

public class MiscKnowledgeBase extends KnowledgeBase {

	private Map<ComponentRef, ComponentDescription> componentInfos = new HashMap<>();
	public Map<ComponentRef, Component> localComponents = new HashMap<>();
	private Set<TrustInfo> trustInfos = new HashSet<>();
	public Set<Info> misc = new HashSet<>();

	public MiscKnowledgeBase(Component component) {
		super(component);

		registerOperation(new add());
		registerOperation(new local());
		registerOperation(new getDescription());
	}

	public class addAll extends TypedInnerClassOperation {
		@Override
		public String getDescription() {
			return null;
		}

		public void addAll(Collection<ComponentDescription> s) {
			s.forEach(i -> considers(i));
		}
	}

	public void considers(ComponentDescription d) {
		var e = componentInfos.get(d.name);

		if (e == null || d.isNewerThan(e)) {
			componentInfos.put(d.name, d);
		}
	}

	public ComponentDescription componentDescriptor() {
		var d = componentInfos.get(component.ref());

		if (d == null) {
			componentInfos.put(component.ref(), d = createLocalComponentDescriptor());
		} else if (d.reliability(component.now()) < 0.1) {
			d.update(createLocalComponentDescriptor());
		}

		return d;
	}

	public ComponentDescription descriptor(ComponentRef id, boolean create) {
		var d = componentInfos.get(id);

		if (d == null && create) {
			componentInfos.put(id, d = new ComponentDescription(component.now()));
			d.name = id;
		}

		return d;
	}

	public List<ComponentDescription> descriptors(ComponentRef... names) {
		List<ComponentDescription> r = new ArrayList<>();

		for (var n : names) {
			r.add(descriptor(n, true));
		}

		return r;
	}

	public ComponentDescription createLocalComponentDescriptor() {
		ComponentDescription d = new ComponentDescription(component.now());
		d.name = component.ref();
		d.location = component.getLocation();
		d.timeModel = component.lookup(TimeService.class).model;
		d.systemInfo = component.lookup(SystemMonitor.class).info.get();
		component.forEachService(s -> d.services.add(s.descriptor()));
		return d;
	}

	public void removeOutdated(double now) {
		removeOutdate(now, misc.iterator());
		removeOutdate(now, trustInfos.iterator());
	}

	private static void removeOutdate(double now, Iterator<? extends Info> i) {

		while (i.hasNext()) {
			var n = i.next();

			if (n.reliability(now) < 0.1) {
				i.remove();
			}
		}
	}

	public class local extends TypedInnerClassOperation {
		public ComponentDescription get() {
			return componentDescriptor();
		}

		@Override
		public String getDescription() {
			return "gives a description of the local component";
		}
	}

	@Override
	public String getFriendlyName() {
		return "knowledge base";
	}

	public class add extends TypedInnerClassOperation {
		@Override
		public String getDescription() {
			return null;
		}

		public void f(Info i) {
			if (i instanceof ComponentDescription) {
				var d = (ComponentDescription) i;
				var alreadyHere = componentInfos.get(d.name);

				if (alreadyHere == null) {
					componentInfos.put(d.name, d);
				} else if (d.isNewerThan(alreadyHere)) {
					alreadyHere.consider(d);
				}
			} else {
				misc.add(i);
			}
		}
	}

	public class getDescription extends TypedInnerClassOperation {
		public ComponentDescription f(ComponentRef c) {
			return componentInfos.get(c);
		}

		@Override
		public String getDescription() {
			return "get the description for a particular component";
		}
	}

	@Override
	protected void forEachInfo(Predicate<Info> c) {
		// TODO Auto-generated method stub

	}

}
