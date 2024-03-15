package idawi.service;

import idawi.Component;
import idawi.Service;
import idawi.service.local_view.ComponentInfo;
import idawi.service.local_view.LocalViewService;

public class DigitalTwinService extends Service {

	private ComponentInfo c = new ComponentInfo(component.now());
	public LocalViewService host;

	public DigitalTwinService(Component c) {
		super(c);
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
