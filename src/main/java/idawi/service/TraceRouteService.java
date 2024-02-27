package idawi.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.routing.Route;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class TraceRouteService extends Service {
	public TraceRouteService(Component node) {
		super(node);
	}

	public class traceroute extends InnerClassEndpoint {
		@Override
		public void impl(MessageQueue in) throws Throwable {
			var msg = in.poll_sync();
			reply(msg, msg.route, true);
		}

		@Override
		public String getDescription() {
			return "returns the route taken by the trigger message";
		}
	}

	public Route traceRoute(Component t, double timeout) {
		return (Route) component.bb().exec_rpc(t, TraceRouteService.class, traceroute.class, null);
	}

	public Map<Component, Route> traceRoute(Set<Component> targets, double timeout) {
		var map = new HashMap<Component, Route>();
		component.bb().exec(TraceRouteService.class, traceroute.class, null, ComponentMatcher.multicast(targets), true,
				null, true).returnQ.collector().collect(timeout, timeout, c -> {
					var target = c.messages.last().route.source();
					var route = (Route) c.messages.last().content;
					map.put(target, route);
					c.stop = c.messages.senders().equals(targets);
				});

		return map;
	}

	@Override
	public String getFriendlyName() {
		return "traceroute";
	}

}
