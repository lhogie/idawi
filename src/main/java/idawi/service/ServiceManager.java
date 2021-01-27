package idawi.service;

import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import idawi.IdawiExposed;
import idawi.Service;
import idawi.ServiceDescriptor;
import toools.reflect.Clazz;

public class ServiceManager extends Service {

	public ServiceManager(Component peer) {
		super(peer);
	}

	@IdawiExposed
	public ServiceDescriptor start(Class<? extends Service> serviceID) {
		if (lookupService(serviceID) != null) {
			throw new IllegalArgumentException("service already running");
		}

		var constructor = Clazz.getConstructor(serviceID, Component.class);

		if (constructor == null) {
			throw new IllegalStateException(
					serviceID + " does not have constructor (" + Component.class.getName() + ")");
		}

		Service s = Clazz.makeInstance(constructor, component);
		return s.descriptor();
	}

	@IdawiExposed
	public void stop(Class<? extends Service> serviceID) {
		Service s = component.lookupService(serviceID);
		component.removeService(s);
	}

	@IdawiExposed
	public Set<String> list() {
		Set<String> r = new HashSet<>();
		component.services().forEach(s -> r.add(s.id.getName()));
		return r;
	}

	@IdawiExposed
	public boolean has(Class serviceID) {
		return lookupService(serviceID) != null;
	}

	@Override
	public String getFriendlyName() {
		return "start/stop services";
	}
}
