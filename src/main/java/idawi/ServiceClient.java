package idawi;

import java.util.Set;

public class ServiceClient extends Service {

	public ServiceClient(Component t) {
		super(t);
	}

	public void startRestServer(Set<ComponentInfo> components, Class<? extends Service> serviceID) {
		rest("start", components, serviceID);
	}

	public void stoptRestServer(Set<ComponentInfo> components, Class<? extends Service> serviceID) {
		rest("stop", components, serviceID);
	}

	private void rest(String cmd, Set<ComponentInfo> components, Class<? extends Service> serviceID) {
		send(null, new To(components, serviceID, cmd + " REST")).collect();
	}
}
