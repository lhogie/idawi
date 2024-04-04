package idawi.service;

import java.util.Set;
import java.util.concurrent.TimeoutException;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.routing.RoutingParameters;
import idawi.routing.RoutingService;

public class PingService extends Service {
	public PingService(Component component) {
		super(component);
	}

	public class ping extends InnerClassEndpoint {

		@Override
		public void impl(MessageQueue in) {
			var m = in.poll_sync();

			// sends back the ping message to the caller
			reply(m, m, true);
		}

		@Override
		public String getDescription() {
			return "sends the message back";
		}
	}

	public static <P extends RoutingParameters> MessageQueue ping(RoutingService<P> r, P p, ComponentMatcher target) {
		return r.exec(target, PingService.class, ping.class, p, "foobar", true).returnQ;
	}

	public static <P extends RoutingParameters> MessageQueue ping(Set<Component> targets, RoutingService<P> r, P p) {
		return ping(r, p, ComponentMatcher.multicast(targets));
	}

	public <P extends RoutingParameters> Message ping(Component target) {
		var routing = component.defaultRoutingProtocol();
		var parms = routing.defaultData();
		var pong = ping(routing, parms, ComponentMatcher.unicast(target)).poll_sync();

		if (pong == null) {
			return null;
		} else {
			return pong.throwIfError();
		}
	}

	public <P extends RoutingParameters> MessageQueue ping(Set<Component> targets) {
		var r = component.defaultRoutingProtocol();
		var p = r.defaultData();
		return ping(r, p, ComponentMatcher.multicast(targets));
	}

}
