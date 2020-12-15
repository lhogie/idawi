package idawi.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Operation;
import idawi.Service;
import idawi.To;

public class ServiceManager extends Service {
	public ServiceManager(Component peer) {
		super(peer);

		registerOperation("start", (msg, out) -> component.addService((Class<Service>) msg.content));

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

	public void start(Class<? extends Service> clazz, ComponentInfo target, double timeoutS) {
		send(clazz, new To(target, ServiceManager.class, "start")).setTimeout(timeoutS).collect();
	}
}
