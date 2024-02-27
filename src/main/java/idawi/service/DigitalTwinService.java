package idawi.service;

import java.util.Objects;

import idawi.Component;
import idawi.Service;
import idawi.service.local_view.ComponentInfo;
import idawi.service.local_view.LocalViewService;

public class DigitalTwinService extends Service {

	private ComponentInfo c = new ComponentInfo(component.now());
	public LocalViewService host;

	public DigitalTwinService(Component c, LocalViewService owner) {
		super(c);
		Objects.requireNonNull(owner);

		if (c == owner.component)
			throw new IllegalArgumentException();

		this.host = owner;
	}

	public void kill() {
		component.bb().exec_rpc(component, ExitApplication.class, ExitApplication.exit.class, null);
	}

	public void retrieveInfo() {
		component.bb().exec_rpc(component, SystemService.class, SystemService.info.class, null);
	}

	public ComponentInfo info() {
		return c;
	}

	public void update(ComponentInfo i) {
		if (c == null) {
			c = i;
		} else {
			c.updateFrom(i);
		}
	}

}
