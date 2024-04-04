package idawi.demo;

import java.util.Random;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import idawi.routing.BlindBroadcasting;
import idawi.routing.ComponentMatcher;
import idawi.routing.FloodingWithSelfPruning;
import idawi.service.PingService;
import idawi.service.PingService.ping;
import idawi.service.local_view.LocalViewService;
import idawi.service.local_view.Network;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.Topologies;
import idawi.transport.TransportService;

public class ManyComponents {
	public static void main(String[] args) throws Throwable {
		System.out.println("start");
		Idawi.agenda.start();
		Idawi.prng = new Random(0);

		// creates components
		var components = Component.createNComponent(100);

		for (var c : components) {
			new SharedMemoryTransport(c);
			new LocalViewService(c);
			c.need(FloodingWithSelfPruning.class);
			c.need(PingService.class);
		}

		// connect them in a random tree
		Topologies.chains(components, 3, Idawi.prng, (a, b) -> SharedMemoryTransport.class, components);

		var first = components.getFirst();
		var last = components.getLast();
		Network.markLinkActive(last, first, SharedMemoryTransport.class, false, components);

//		Idawi.directory = new Directory("$HOME/test/idawi");
//		GraphvizDriver.path = "/usr/local/bin/";
//		first.localView().g.plot().open();
		var to = ComponentMatcher.unicast(components.get(components.size() / 2));
		var q = first.need(BlindBroadcasting.class).exec(to, PingService.class, ping.class, null, "ping",
				true).returnQ;
		var pong = q.poll_sync().throwIfError();
		System.out.println("pong= " + pong);
		System.out.println("ping travelled through: " + ((Message) pong.content).route.components());

		Idawi.agenda.stopNow(null);
		System.out.println("nbReceptions: "
				+ components.stream().mapToLong(c -> c.need(TransportService.class).nbMsgReceived).sum());
	}
}
