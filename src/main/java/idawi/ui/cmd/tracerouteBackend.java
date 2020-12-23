package idawi.ui.cmd;

import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Message;
import idawi.service.PingPong;
import idawi.service.registry.RegistryService;

public class tracerouteBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out) throws Throwable {
		var parms = cmdline.findParameters();
		Set<ComponentInfo> to = n.lookupService(RegistryService.class).lookupByRegexp(parms.get(0));

		for (ComponentInfo t : to) {
			out.accept("ping " + t);
			Message pong = n.lookupService(PingPong.class).ping(t, 1000);

			if (pong == null) {
				out.accept("No pong received. :(");
				return;
			}

			Message ping = (Message) pong.content;
			out.accept("forward route:  " + ping.route);
			out.accept("backward route: " + pong.route);
			double time = pong.receptionDate - ((Message) pong.content).emissionDate;
			out.accept("time: " + time + "s");
		}
	}
}
