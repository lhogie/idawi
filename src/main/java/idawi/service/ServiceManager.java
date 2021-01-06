package idawi.service;

import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Operation;
import idawi.Service;
import idawi.ServiceDescriptor;
import toools.reflect.Clazz;

public class ServiceManager extends Service {

	public static class Stub extends ServiceStub {
		public Stub(Service localService, Set<ComponentDescriptor> remoteComponents) {
			super(localService, remoteComponents, ServiceManager.class);
		}

		public void start(Class<? extends Service> serviceID) throws Throwable {
			localService.call(to(start), serviceID).collect().throwAnyError();
		}

		public void stop(Class<? extends Service> serviceID) throws Throwable {
			localService.call(to(stop), serviceID).collect().throwAnyError();
		}

		public boolean has(Class<? extends Service> serviceID) throws Throwable {
			return (Boolean) localService.call(to(has), serviceID).get();
		}

		public Set<String> list() throws Throwable {
			return (Set<String>) localService.call(to(list)).get();
		}
	}

	public ServiceManager(Component peer) {
		super(peer);
	}

	public final static String start = "start";
	public final static String stop = "stop";
	public final static String list = "list";
	public final static String has = "has";

	@Operation
	public ServiceDescriptor start(Class<? extends Service> serviceID) {
		if (service(serviceID) != null) {
			throw new IllegalArgumentException("service already running");
		}

		var constructor = Clazz.getConstructor(serviceID, Component.class);

		if (constructor == null) {
			throw new IllegalStateException(serviceID + " does not have constructor (" + Component.class.getName() + ")");
		}

		Service s = Clazz.makeInstance(constructor, component);
		return s.descriptor();
	}

	@Operation
	public void stop(Class<? extends Service> serviceID) {
		Service s = component.lookupService(serviceID);
		component.removeService(s);
	}

	@Operation
	public Set<String> list() {
		Set<String> r = new HashSet<>();
		component.services().forEach(s -> r.add(s.id.getName()));
		return r;
	}

	@Operation
	public boolean has(Class serviceID) {
		return service(serviceID) != null;
	}

	@Override
	public String getFriendlyName() {
		return "start/stop services";
	}
}
