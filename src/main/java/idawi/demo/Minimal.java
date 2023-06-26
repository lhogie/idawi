package idawi.demo;

import java.io.IOException;
import java.util.stream.Stream;

import idawi.Component;
import idawi.routing.BlindBroadcasting;
import idawi.routing.ComponentMatcher;
import idawi.routing.RoutingListener;
import idawi.routing.RoutingService;
import idawi.transport.SharedMemoryTransport;
import toools.thread.Threads;

public class Minimal {
	public static void main(String[] args) throws IOException {
		var a = new Component("a");
		var b = new Component("b");

		var rl = new RoutingListener.Stdout();
		Stream.of(a, b).forEach(c -> c.bb().listeners.add(rl));

		System.out.println(a.services());

		a.need(SharedMemoryTransport.class).outTo(b.need(SharedMemoryTransport.class));

		var r = a.bb();
		System.out.println("routing: " + r);
		r.exec(BlindBroadcasting.class, RoutingService.test2.class, null, ComponentMatcher.regex("b"), true, null);
//		var pong = r.ping(b).poll_sync();
//		System.out.println("pong= " + pong);
		
		Threads.sleep(1);
		System.out.println(a.bb().alreadyReceivedMsgs);
	}
}
