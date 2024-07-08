package idawi.demo;

import java.util.List;

import idawi.Component;
import idawi.Idawi;
import idawi.service.PingService;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.TransportListener;
import idawi.transport.TransportService;
import jdotgen.GraphvizDriver;
import toools.io.file.Directory;

public class JustAPingToOneNeighbor {

	public static void main(String[] args) throws Throwable {
		GraphvizDriver.path = "/usr/local/bin/";

		Idawi.directory = new Directory("$HOME/test/idawi");
		Idawi.directory.mkdirs();
//		Idawi.agenda.listeners.add(new AgendaListener.PrintStreamRuntimeListener(System.out));

		var a = new Component();
		var b = new Component();
		b.need(PingService.class);

//		var rl = new RoutingListener.PrintTo(System.out);
//		Stream.of(a, b, c).forEach(u -> u.bb().listeners.add(rl));
//		Stream.of(a, b, c).forEach(u -> u.bb());

		// let a and b know that there exists a a->b link
		a.localView().g.markLinkActive(a, b, SharedMemoryTransport.class, false);
		b.localView().g.markLinkActive(b, a, SharedMemoryTransport.class, false);

		List.of(a, b).stream().flatMap(c -> c.services(TransportService.class).stream())
				.forEach(t -> t.listeners.add(new TransportListener.StdOut()));

		Idawi.agenda.start();

		System.out.println("pinging");
		var pong = a.defaultRoutingProtocol().ping(b);

		System.out.println("pong= " + pong);

		Idawi.agenda.stopNow(null);
	}
}
