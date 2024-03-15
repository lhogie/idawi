package idawi.demo;

import idawi.Component;
import idawi.Idawi;
import idawi.routing.ComponentMatcher;
import idawi.routing.RoutingService;
import idawi.transport.SharedMemoryTransport;

public class JustAPing {

	public static void main(String[] args) throws Throwable {
		var a = new Component();
		a.friendlyName = "a";
		var b = new Component();
		b.friendlyName = "b";
		b.bb();

		a.localView().g.markLinkActive(a, b, SharedMemoryTransport.class, false);

		Idawi.agenda.setTerminationCondition(() -> Idawi.agenda.now() > 1);
		Idawi.agenda.start();
		a.bb().exec(RoutingService.class, RoutingService.dummyService.class, null, ComponentMatcher.unicast(b), false,
				null, true);
		Idawi.agenda.waitForCompletion();
	}
}
