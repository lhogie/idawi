package idawi.service;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Message;
import idawi.MessageException;
import idawi.Operation;
import idawi.Service;
import idawi.To;
import toools.reflect.Clazz;

public class ServiceManager extends Service {

	public static class Stub extends ComponentStub<ServiceManager> {
		public Stub(ServiceManager localService, ComponentInfo remoteComponent) {
			super(localService, remoteComponent);
		}

		public void start(Class<? extends Service> c) throws MessageException {
			localService.start(remoteComponent, c);
		}

		public void stop(Class<? extends Service> c) throws MessageException {
			localService.stop(remoteComponent, c);
		}

		public Set<String> list() throws MessageException {
			return ServiceManager.list(localService, remoteComponent);
		}
	}

	public ServiceManager(Component peer) {
		super(peer);
	}

	public static String start = "start";

	@Operation
	private void start(Message msg, Consumer<Object> returns) {
		if (service(id) != null) {
			throw new IllegalArgumentException("service already running");
		}

		var constructor = Clazz.getConstructor(id, Component.class);

		if (constructor == null) {
			throw new IllegalStateException(id + " does not have constructor (" + Component.class.getName() + ")");
		}

		Service s = Clazz.makeInstance(constructor, this);
	}

	public void start(Set<ComponentInfo> targets, Class<? extends Service> clazz) throws MessageException {
		send(clazz, new To(targets, ServiceManager.class, start)).collect().throwAnyError();
	}

	@Operation
	private void stop(Class<Service> clazz) {
		Service s = component.lookupService(id);
		component.removeService(s);
	}

	public void stop(Set<ComponentInfo> targets, Class<? extends Service> c) throws MessageException {
		send(c, new To(targets, ServiceManager.class, "stop")).collect().throwAnyError();
	}

	@Operation
	private Set<String> list() {
		Set<String> r = new HashSet<>();
		component.services().forEach(s -> r.add(s.id.getName()));
		return r;
	}

	public static Set<String> list(Service localService, Set<ComponentInfo> targets) throws MessageException {
		return (Set<String>) localService.send(null, new To(targets, ServiceManager.class, "list")).collect()
				.throwAnyError().resultMessages().first().content;
	}

	@Override
	public String getFriendlyName() {
		return "start/stop services";
	}
}
