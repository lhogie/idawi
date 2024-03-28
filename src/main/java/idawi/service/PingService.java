package idawi.service;

import java.util.Set;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.routing.RoutingParameters;
import idawi.routing.RoutingService;
import toools.io.Cout;

public class PingService extends Service {
	public PingService(Component component) {
		super(component);
	}

	public class ping extends InnerClassEndpoint {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var m = in.poll_sync();
			// sends back the ping message to the caller
			reply(m, m.route, true);
		}

		@Override
		public String getDescription() {
			return "sends back the exec message";
		}
	}

	public static <P extends RoutingParameters> MessageQueue ping(RoutingService<P> r, P p, ComponentMatcher target) {
		return r.exec(target, PingService.class, ping.class, p, "foobar", true).returnQ;
	}

	public static <P extends RoutingParameters> MessageQueue ping(Set<Component> targets, RoutingService<P> r, P p) {
		return ping(r, p, ComponentMatcher.multicast(targets));
	}

	public static <P extends RoutingParameters> MessageQueue pingAround(RoutingService<P> r, P p) {
		return ping(r, p, ComponentMatcher.all);
	}

	public <P extends RoutingParameters> MessageQueue ping(Component target) {
		var routing = component.bb();
		var parms = routing.defaultParameters();
		return ping(routing, parms, ComponentMatcher.unicast(target));
	}
	public <P extends RoutingParameters> MessageQueue ping(Set<Component> targets ) {
		var r = component.bb();
		var p = r.defaultParameters();
		return ping(r, p, ComponentMatcher.multicast(targets));
	}

}
