package idawi.knowledge_base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import idawi.Component;
import idawi.TypedInnerClassOperation;
import idawi.service.SystemMonitor;
import idawi.service.time.TimeService;
import idawi.transport.PipeFromToParentProcess;
import toools.exceptions.NotYetImplementedException;

public class MiscKnowledgeBase extends KnowledgeBase {

	private Set<TrustInfo> trustInfos = new HashSet<>();
	public Set<Info> misc = new HashSet<>();

	public MiscKnowledgeBase(Component component) {
		super(component);

		registerOperation(new add());
		registerOperation(new local());
		registerOperation(new getInfo());
	}

	public class addAll extends TypedInnerClassOperation {
		@Override
		public String getDescription() {
			return null;
		}

		public void addAll(Collection<ComponentInfo> s) {
			s.forEach(i -> considers(i));
		}
	}

	public void considers(ComponentInfo d) {
		var c = component.digitalTwinService().localTwin(d.component);
		c.info = d;
	}

	public ComponentInfo componentInfo() {
		if (component.info == null) {
			component.info = createLocalComponentDescriptor();
		} else if (component.info.reliability(component.now()) < 0.1) {
			component.info.update(createLocalComponentDescriptor());
		}

		return component.info;
	}

	public List<ComponentInfo> descriptors(Component... names) {
		List<ComponentInfo> r = new ArrayList<>();

		for (var n : names) {
			r.add(n.info);
		}

		return r;
	}

	public ComponentInfo createLocalComponentDescriptor() {
		ComponentInfo d = new ComponentInfo(component.now());
		d.component = component;
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
		public ComponentInfo get() {
			return componentInfo();
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
			if (i instanceof ComponentInfo) {
				var d = (ComponentInfo) i;
				var c = component.digitalTwinService().localTwin(d.component);

				if (c.info == null) {
					c.info = d;
				} else if (d.isNewerThan(c.info)) {
					c.info.update(i);
				}
			} else {
				misc.add(i);
			}
		}
	}

	public class getInfo extends TypedInnerClassOperation {
		public ComponentInfo f(Component c) {
			return component.digitalTwinService().localTwin(c).info;
		}

		@Override
		public String getDescription() {
			return "get the description for a particular component";
		}
	}

	@Override
	protected void forEachInfo(Predicate<Info> c) {
		throw new NotYetImplementedException();
	}

	public void considers(PipeFromToParentProcess descr) {
		
	}

}
