package idawi.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.InnerOperation;
import idawi.MessageQueue;
import idawi.Route;
import idawi.Service;
import idawi.To;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class TraceRouteService extends Service {
	public TraceRouteService(Component node) {
		super(node);
		registerOperation(new traceroute());
	}

	public class traceroute extends InnerOperation {
		@Override
		public void exec(MessageQueue in) throws Throwable {
			var msg = in.get_blocking();
			reply(msg, msg.route);
		}

		@Override
		public String getDescription() {
			return "returns the route taken by the trigger message";
		}
	}

	public Route traceRoute(ComponentDescriptor t, double timeout) {
		return (Route) exec(new To(t).o(TraceRouteService.traceroute.class), true, null).returnQ
				.get_blocking(timeout).content;
	}

	public Map<ComponentDescriptor, Route> traceRoute(Set<ComponentDescriptor> targets, double timeout) {
		var map = new HashMap<ComponentDescriptor, Route>();
		exec(new To(targets).o(TraceRouteService.traceroute.class), true, null).returnQ.collect(timeout, timeout, c -> {
			var target = c.messages.last().route.source().component;
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
