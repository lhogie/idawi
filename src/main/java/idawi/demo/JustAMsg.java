package idawi.demo;

import idawi.Component;
import idawi.Idawi;
import idawi.routing.ComponentMatcher;
import idawi.routing.RoutingService;
import idawi.routing.RoutingService.dummyService;
import idawi.transport.SharedMemoryTransport;

public class JustAMsg {

	public static void main(String[] args) throws Throwable {
		var a = new Component();
		var b = new Component();
		b.bb();
		
		a.localView().g.markLinkActive(a, b, SharedMemoryTransport.class, false);

		Idawi.agenda.setTerminationCondition(() -> Idawi.agenda.now() > 1);
		Idawi.agenda.start();
		a.bb().exec(ComponentMatcher.unicast(b), RoutingService.class, dummyService.class, null, null, true);
		Idawi.agenda.scheduleTerminationAt(2, () -> {
		});
		Idawi.agenda.waitForCompletion();
		System.out.println("done");
	}
}
