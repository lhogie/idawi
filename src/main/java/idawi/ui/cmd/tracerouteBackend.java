package idawi.ui.cmd;

import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.knowledge_base.ComponentRef;
import idawi.knowledge_base.MapService;
import idawi.messaging.Message;
import idawi.routing.Route;

public class tracerouteBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out) throws Throwable {
		var parms = cmdline.findParameters();
		Set<ComponentRef> to = n.lookup(MapService.class).lookupByRegexp(parms.get(0));

		for (ComponentRef t : to) {
			out.accept("ping " + t);
			Message pong = new Component().bb().ping(t).poll_sync();

			if (pong == null) {
				out.accept("No pong received. :(");
				return;
			}

			var ping = (Route) pong.content;
			out.accept("forward route:  " + ping);
			out.accept("backward route: " + pong.route);
			double time = pong.route.lastReception().date() - ping.initialEmission.date();
			out.accept("time: " + time + "s");
		}
	}
}
