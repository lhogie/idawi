package idawi.map;

import idawi.Component;
import idawi.InnerClassTypedOperation;
import idawi.Route;
import idawi.RouteEntry;
import idawi.Service;
import toools.thread.Threads;

public class MapService extends Service {
	static {
		Threads.newThread_loop(1000, () -> true, () -> {
			Component.componentsInThisJVM.values().forEach(c -> c.lookupService(MapService.class).map.removeOutdated());
		});
	}
	
	private final NetworkMap map = new NetworkMap();

	public MapService(Component component) {
		super(component);
	}

	public class get extends InnerClassTypedOperation {
		public NetworkMap get() {
			return map;
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public String getFriendlyName() {
		return "graph map";
	}

	public void feedWith(Route route) {
		int len = route.size();

		for (int i = 0; i < len; ++i) {
			RouteEntry e = route.get(i);

			if (i > 0) {
				map.add(route.get(i - 1).component, route.get(i - 1).protocolName, e.component);
			}

			if (i < len - 1) {
				map.add(e.component, e.protocolName, route.get(i + 1).component);
			}
		}
	}
}
