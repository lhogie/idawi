package idawi.demo;

import idawi.Component;
import idawi.Idawi;
import idawi.routing.RoutingService;
import idawi.routing.RoutingService.testEndpoint;
import idawi.transport.SharedMemoryTransport;

public class JustAMsg {

	public static void main(String[] args) throws Throwable {
		var a = new Component();
		var b = new Component();
		b.bb();

		a.localView().g.markLinkActive(a, b, SharedMemoryTransport.class, false);

		Idawi.agenda.start();
		a.bb().exec(b, RoutingService.class, testEndpoint.class, null);
		Idawi.agenda.stopWhen(() -> Idawi.agenda.time() >= 2, () -> System.out.println("done"));
	}
}
