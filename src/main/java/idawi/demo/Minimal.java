package idawi.demo;

import java.util.Set;
import java.util.stream.Stream;

import idawi.Component;
import idawi.Idawi;
import idawi.routing.FloodingWithSelfPruning;
import idawi.routing.RoutingListener;
import idawi.service.local_view.Network;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.TransportService;
import toools.thread.Threads;

public class Minimal {
	public static void main(String[] args) throws Throwable {
		Idawi.agenda.start();
		var a = new Component();
		var b = new Component();

		var rl = new RoutingListener.PrintTo(System.out);
		Stream.of(a, b).forEach(c -> c.bb().listeners.add(rl));

		System.out.println(a.services());

		Network.markLinkActive(a, b, SharedMemoryTransport.class, true, Set.of(a, b));

		var r = a.service(FloodingWithSelfPruning.class, true);
		System.out.println("routing: " + r);
//		r.exec(BlindBroadcasting.class, RoutingService.test2.class, null, ComponentMatcher.regex("b"), true, null);
		var pong = r.ping(b).poll_sync();
		System.out.println("pong= " + pong);

		Threads.sleep(1);
		System.out.println(
				"nbMsgSent: " + TransportService.sum(Set.of(a, b), SharedMemoryTransport.class, t -> t.nbMsgSent));
		Idawi.agenda.stop();
	}
}
