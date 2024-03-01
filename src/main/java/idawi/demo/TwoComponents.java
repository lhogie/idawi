package idawi.demo;

import java.io.IOException;
import java.util.Set;

import idawi.Component;
import idawi.RemotelyRunningEndpoint;
import idawi.Idawi;
import idawi.service.DemoService;
import idawi.transport.SharedMemoryTransport;

public class TwoComponents {
	public static void main(String[] args) throws IOException {
		var a = new Component();
		var b = new Component();

		// Network.markLinkActive(a, b, t, true, Set.of(a, b));
		Set.of(a, b).forEach(c -> c.localView().g.markLinkActive(a, b, SharedMemoryTransport.class, true));

		System.out.println(a.localView().localLinks());
		System.out.println(b.localView().localLinks());

		a.service(DemoService.class, true);
		b.service(DemoService.class, true);
		RemotelyRunningEndpoint r = a.bb().exec(DemoService.class, DemoService.stringLength.class, "salut", true);

		System.out.println("collecting");
		r.returnQ.collector().collect(c -> {
			System.out.println("from " + c.messages.last().route.source() + ": " + c.messages.last().content);
		});
		System.out.println("done");
		Idawi.agenda.threadPool.shutdown();
	}
}
