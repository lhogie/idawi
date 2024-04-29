package idawi.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.Endpoint;
import idawi.FunctionEndPoint;
import idawi.ProcedureEndpoint;
import idawi.Service;
import idawi.SupplierEndPoint;
import idawi.routing.RoutingService;

public class ServiceManager extends Service {

	public ServiceManager(Component peer) {
		super(peer);
	}

	public class listRoutingServices extends SupplierEndPoint<List<?>> {
		@Override
		public List<?> get() {
			return component.services(RoutingService.class).stream().map(s -> s.getClass()).toList();
		}

		@Override
		public String getDescription() {
			return "listRoutingServices";
		}
	}

	public class listServices<S extends Service> extends SupplierEndPoint<List<Class<S>>> {
		public List<Class<S>> get() {
			return component.services().stream().map(s -> (Class<S>) s.getClass()).toList();
		}

		@Override
		public String getDescription() {
			return "gives the ID the services available on the local component";
		}
	}

	public class start extends ProcedureEndpoint<Class<? extends Service>> {
		@Override
		public void doIt(Class<? extends Service> serviceID) {
			component.start(serviceID).descriptor();
		}

		@Override
		public String getDescription() {
			return "starts the given service";
		}
	}

	public class stop extends ProcedureEndpoint<Class<? extends Service>> {
		@Override
		public void doIt(Class<? extends Service> serviceID) {
			component.services(serviceID).forEach(s -> s.dispose());
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class list extends SupplierEndPoint<Set<String>> {
		public Set<String> get() {
			return component.services().stream().map(s -> s.getClass().getName()).collect(Collectors.toSet());
		}

		@Override
		public String getDescription() {
			return "services available in this component";
		}
	}

	public class has extends FunctionEndPoint<Class, Boolean> {
		@Override
		public Boolean f(Class serviceID) {
			return component.service(serviceID) != null;
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class getParmTypes extends FunctionEndPoint<Class<? extends Endpoint>, Class<?>> {
		@Override
		public Class<?> f(Class<? extends Endpoint> e) {
			return Endpoint.inputSpecification(e);
		}

		@Override
		public String getDescription() {
			return "gets the parameter types for the given endpoint";
		}
	}

	public class ensureStarted extends ProcedureEndpoint<Class> {
		@Override
		public void doIt(Class serviceID) {
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
