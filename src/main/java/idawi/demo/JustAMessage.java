package idawi.demo;

import java.util.List;
import java.util.stream.Stream;

import idawi.AgendaListener;
import idawi.Component;
import idawi.Idawi;
import idawi.routing.RoutingListener;
import idawi.service.local_view.LocalViewService;
import idawi.transport.SharedMemoryTransport;
import jdotgen.GraphvizDriver;
import toools.io.file.Directory;

public class JustAMessage {

	public static void main(String[] args) throws Throwable {
		GraphvizDriver.path = "/usr/local/bin/";

		Idawi.directory = new Directory("$HOME/test/idawi");
		Idawi.directory.mkdirs();
		Idawi.agenda.listeners.add(new AgendaListener.PrintStreamRuntimeListener(System.out));

		var a = new Component();
		var b = new Component();
		var c = new Component();

		var rl = new RoutingListener.PrintTo(System.out);
		Stream.of(a, b, c).forEach(u -> u.bb().listeners.add(rl));

		// let a and b know that there exists a a->b link
		a.localView().g.markLinkActive(a, b, SharedMemoryTransport.class, false);
		b.localView().g.markLinkActive(b, c, SharedMemoryTransport.class, false);
		c.localView().g.markLinkActive(c, a, SharedMemoryTransport.class, false);

		List.of(a, b, c).forEach(u -> System.out.println(
				u + " knows: " + u.service(LocalViewService.class).g.links().stream().map(l -> l.dest.component).toList()));

		Idawi.agenda.start();

		System.out.println("ping");
		var pong = a.bb().ping(b).poll_sync();
		System.out.println("pong= " + pong);

		Idawi.agenda.stop();

		System.out.println("alreadyReceivedMsgs=" + a.alreadyKnownMsgs);
	}
}
