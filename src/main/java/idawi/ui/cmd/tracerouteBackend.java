package idawi.ui.cmd;

import java.util.function.Consumer;

import idawi.Component;
import idawi.messaging.Message;
import idawi.routing.Route;
import idawi.service.local_view.LocalViewService;

public class tracerouteBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out) throws Throwable {
		var parms = cmdline.findParameters();
		var to = n.service(LocalViewService.class).g.lookupByRegexp(parms.get(0));

		for (var t : to) {
			out.accept("ping " + t);
			Message pong = n.bb().ping(t).poll_sync();

			if (pong == null) {
				out.accept("No pong received. :(");
				return;
			}

			var ping = (Route) pong.content;
			out.accept("forward route:  " + ping);
			out.accept("backward route: " + pong.route);
			double time = pong.route.last().receptionDate - ping.first().emissionDate;
			out.accept("time: " + time + "s");
		}
	}
}
