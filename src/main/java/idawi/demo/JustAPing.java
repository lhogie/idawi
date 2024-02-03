package idawi.demo;

import java.util.Set;
import java.util.stream.Stream;

import idawi.Component;
import idawi.Idawi;
import idawi.Agenda;
import idawi.routing.RoutingListener;
import idawi.service.local_view.LocalViewService;
import idawi.service.local_view.Network;
import idawi.transport.SharedMemoryTransport;

public class JustAPing {
	public static void main(String[] args) throws Throwable {
		var a = new Component("a");
		var b = new Component("b");

		var rl = new RoutingListener.Stdout();
		Stream.of(a, b).forEach(c -> c.bb().listeners.add(rl));


		Network.markLinkActive(a, b, SharedMemoryTransport.class, false, Set.of(a, b));

		System.out.println(a + " knows: " + a.service(LocalViewService.class).g.links());
		System.out.println(b + " knows: " + b.service(LocalViewService.class).g.links());
		
		Idawi.agenda.processEventQueue();
		
		System.out.println("ping");
		var pong = a.bb().ping(b).poll_sync();
		System.out.println("pong= " + pong);

		System.out.println("alreadyReceivedMsgs=" + a.bb().alreadyReceivedMsgs);
	}
}
