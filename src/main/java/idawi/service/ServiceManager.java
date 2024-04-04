package idawi.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.routing.RoutingService;
import idawi.service.local_view.ServiceInfo;

public class ServiceManager extends Service {

	public ServiceManager(Component peer) {
		super(peer);
	}

	public class listRoutingServices extends TypedInnerClassEndpoint {
		public List<?> f() {
			return component.services(RoutingService.class).stream().map(s -> s.getClass()).toList();
		}

		@Override
		public String getDescription() {
			return "listRoutingServices";
		}
	}

	public class listServices extends TypedInnerClassEndpoint {
		public <S extends Service> List<Class<S>> f() {
			return component.services().stream().map(s -> (Class<S>) s.getClass()).toList();
		}

		@Override
		public String getDescription() {
			return "gives the ID the services available on the local component";
		}
	}

	public class start extends TypedInnerClassEndpoint {
		public ServiceInfo f(Class<? extends Service> serviceID) {
			return component.start(serviceID).descriptor();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class stop extends TypedInnerClassEndpoint {
		public void stop(Class<? extends Service> serviceID) {
			component.services(serviceID).forEach(s -> s.dispose());
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class list extends TypedInnerClassEndpoint {
		public Set<String> get() {
			return component.services().stream().map(s -> s.getClass().getName()).collect(Collectors.toSet());
		}

		@Override
		public String getDescription() {
			return "list services available in this component";
		}
	}

	public class has extends TypedInnerClassEndpoint {
		public boolean has(Class serviceID) {
			return component.service(serviceID) != null;
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class ensureStarted extends TypedInnerClassEndpoint {
		public void f(Class serviceID) {
//			Cout.debugSuperVisible("ensure started " + serviceID);
			component.service(serviceID);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@Override
	public String getFriendlyName() {
		return "start/stop services";
	}

}
