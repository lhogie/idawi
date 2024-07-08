package idawi.demo;

import java.util.Set;

import idawi.Component;
import idawi.Computation;
import idawi.Idawi;
import idawi.service.DemoService;
import idawi.transport.SharedMemoryTransport;
import toools.io.Cout;

public class TwoComponents {
	public static void main(String[] args) throws Throwable {
		var a = new Component();
		var b = new Component();
		Idawi.agenda.start();

		// Network.markLinkActive(a, b, t, true, Set.of(a, b));
		Set.of(a, b).forEach(c -> c.localView().g.markLinkActive(a, b, SharedMemoryTransport.class, true));

		a.need(DemoService.class);
		b.need(DemoService.class);
		
		Computation r = a.defaultRoutingProtocol().exec(b, DemoService.class, DemoService.stringLength.class, msg -> msg.content = "salut");

		System.out.println("collecting in  " + r.returnQ);
		r.returnQ.collector().collect(c -> {
			Cout.debug("got " + c.messages.last().content);
			System.out.println("from " + c.messages.last().route.source() + ": " + c.messages.last().content);
			c.gotEnough = true;
		});
		System.out.println("done");

		Idawi.agenda.stopNow(null);
	}
}
