package idawi.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.ComponentAddress;
import idawi.IdawiOperation;
import idawi.OperationParameterList;
import idawi.Service;
import idawi.ServiceAddress;
import idawi.ServiceDescriptor;
import idawi.ServiceStub;
import toools.reflect.Clazz;

public class ServiceManager extends Service {

	public static class Stub extends ServiceStub {

		public Stub(Service localService, ComponentAddress remoteComponents) {
			super(localService, new ServiceAddress(remoteComponents, ServiceManager.class));
		}

		public List<String> list() {
			return (List<String>) (List) localService.start(to, list, true, new OperationParameterList()).returnQ
					.collect().contents();
		}

		public boolean has(Class<? extends Service> s) {
			return localService.start(to, has, true, s).returnQ.collect().contents().contains(true);
		}

		public void start(Class<? extends Service> s) {
			localService.start(to, start, true, s).returnQ.collect();
		}

		public void stop(Class<? extends Service> s) {
			localService.start(to, stop, true, s).returnQ.collect();
		}
	}

	public ServiceManager(Component peer) {
		super(peer);
	}

	public static OperationID start;

	@IdawiOperation
	public ServiceDescriptor start(Class<? extends Service> serviceID) {
		if (lookupService(serviceID) != null) {
			throw new IllegalArgumentException("service already running");
		}

		var constructor = Clazz.getConstructor(serviceID, Component.class);

		if (constructor == null)
			throw new IllegalStateException(
					serviceID + " does not have constructor (" + Component.class.getName() + ")");

		Service s = Clazz.makeInstance(constructor, this.component);
		return s.descriptor();
	}

	public static OperationID stop;

	@IdawiOperation
	public void stop(Class<? extends Service> serviceID) {
		Service s = component.lookupService(serviceID);
		component.removeService(s);
	}

	public static OperationID list;

	@IdawiOperation
	public Set<String> list() {
		Set<String> r = new HashSet<>();
		component.services().forEach(s -> r.add(s.id.getName()));
		return r;
	}

	public static OperationID has;

	@IdawiOperation
	public boolean has(Class serviceID) {
		return lookupService(serviceID) != null;
	}

	public static OperationID ensureStarted;

	@IdawiOperation
	public void ensureStarted(Class serviceID) {
		if (!has(serviceID)) {
			start(serviceID);
		}
	}

	@Override
	public String getFriendlyName() {
		return "start/stop services";
	}

}
