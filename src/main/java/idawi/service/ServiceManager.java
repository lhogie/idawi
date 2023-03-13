package idawi.service;

import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassOperation;
import idawi.knowledge_base.ServiceDescriptor;
import toools.reflect.Clazz;

public class ServiceManager extends Service {

	public ServiceManager(Component peer) {
		super(peer);
		registerOperation(new ensureStarted());
		registerOperation(new has());
		registerOperation(new list());
		registerOperation(new start());
		registerOperation(new stop());
	}

	public class start extends TypedInnerClassOperation {
		public ServiceDescriptor f(Class<? extends Service> serviceID) {
			if (component.lookup(serviceID) != null) {
				throw new IllegalArgumentException("service already running");
			}

			var constructor = Clazz.getConstructor(serviceID, Component.class);

			if (constructor == null)
				throw new IllegalStateException(
						serviceID + " does not have constructor (" + Component.class.getName() + ")");

			Service s = Clazz.makeInstance(constructor, component);
			return s.descriptor();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class stop extends TypedInnerClassOperation {
		public void stop(Class<? extends Service> serviceID) {
			Service s = component.lookup(serviceID);
			component.removeService(s);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class list extends TypedInnerClassOperation {
		public Set<String> list() {
			Set<String> r = new HashSet<>();
			component.forEachService(s -> r.add(s.id.getName()));
			return r;
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class has extends TypedInnerClassOperation {
		public boolean has(Class serviceID) {
			return component.lookup(serviceID) != null;
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public void ensureStarted(Class serviceID) {
		lookup(ensureStarted.class).f(serviceID);
	}

	public class ensureStarted extends TypedInnerClassOperation {
		public void f(Class serviceID) {
//			Cout.debugSuperVisible("ensure started " + serviceID);
			if (!lookup(has.class).has(serviceID)) {
				lookup(start.class).f(serviceID);
			}
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public String getFriendlyName() {
		return "start/stop services";
	}

}
