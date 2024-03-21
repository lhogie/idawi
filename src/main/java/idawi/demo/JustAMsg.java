package idawi.demo;

import idawi.Component;
import idawi.Idawi;
import idawi.routing.ComponentMatcher;
import idawi.routing.RoutingService;
import idawi.transport.SharedMemoryTransport;

public class JustAMsg {

	public static void main(String[] args) throws Throwable {
		var a = new Component();
		var b = new Component();
		b.bb();

		a.localView().g.markLinkActive(a, b, SharedMemoryTransport.class, false);

		Idawi.agenda.setTerminationCondition(() -> Idawi.agenda.now() > 1);
		Idawi.agenda.start();
		a.bb().exec(RoutingService.class, RoutingService.dummyService.class, null, ComponentMatcher.unicast(b), false,
				null, true);
		Idawi.agenda.scheduleTerminationAt(2, () -> {});
		Idawi.agenda.waitForCompletion();
		System.out.println("done");
	}
}
