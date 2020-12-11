package idawi.service;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Operation;
import idawi.Service;
import idawi.To;
import toools.reflect.Clazz;

public class ServiceManager extends Service {
	public ServiceManager(Component peer) {
		super(peer);

		registerOperation("start", (msg, out) -> {
			Class<Service> clazz = (Class<Service>) msg.content;
			Constructor<Service> constructor = Clazz.getConstructor(clazz, Component.class);
			Service service = Clazz.makeInstance(constructor, component);
			component.services().add(service);
		});

		registerOperation("stop", (msg, out) -> {
			Class<Service> clazz = (Class<Service>) msg.content;
			List<Service> serviceToStop = new ArrayList<>();

			for (Service s : component.services()) {
				if (clazz.isAssignableFrom(s.getClass())) {
					serviceToStop.add(s);
				}
			}

			serviceToStop.forEach(s -> {
				s.shutdown();
				component.removeService(s);
			});
		});
	}

	@Operation
	public Set<String> list() {
		Set<String> r = new HashSet<>();
		component.services().forEach(s -> r.add(s.id.getName()));
		return r;
	}

	@Override
	public String getFriendlyName() {
		return "start/stop services";
	}

	public void start(Class<? extends Service> clazz, ComponentInfo p) {
		send(clazz, new To(p, ServiceManager.class, "start")).collect();
	}

	public static void start(Class<? extends Service> clazz, Service localService, ComponentInfo p) {
		To to = new To(Set.of(p), ServiceManager.class, "start");
		localService.send(clazz, to).collect();
	}
}
