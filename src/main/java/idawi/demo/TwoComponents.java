package idawi.demo;

import java.util.Set;

import idawi.Component;
import idawi.Idawi;
import idawi.RemotelyRunningEndpoint;
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
		RemotelyRunningEndpoint r = a.bb().exec(DemoService.class, DemoService.stringLength.class, "salut", true);

		System.out.println("collecting in  " + r.returnQ);
		r.returnQ.collector().collect(c -> {
			Cout.debug("got " + c.messages.last().content);
			System.out.println("from " + c.messages.last().route.source() + ": " + c.messages.last().content);
			c.stop = true;
		});
		System.out.println("done");

		Idawi.agenda.stopNow(null);
	}
}
