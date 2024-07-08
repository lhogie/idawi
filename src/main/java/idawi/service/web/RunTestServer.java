package idawi.service.web;

import java.util.ArrayList;
import java.util.Random;

import idawi.Component;
import idawi.Idawi;
import idawi.routing.BFSRouting;
import idawi.routing.BlindBroadcasting;
import idawi.routing.ProbabilisticBroadcasting;
import idawi.service.DemoService;
import idawi.service.PingService;
import idawi.service.ServiceManager;
import idawi.transport.Loopback;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.Topologies;
import idawi.transport.TransportListener;
import idawi.transport.TransportService;

public class RunTestServer {

	public static void main(String[] args) throws Throwable {
		int n = 5;
		var components = new ArrayList<Component>();
		components.addAll(Component.createNComponent(n));

		for (int i = 0; i < n; ++i) {
			var c = components.get(i);
			c.friendlyName = "c" + i;
			c.need(Loopback.class, DemoService.class, ServiceManager.class, PingService.class);
		}

		Topologies.tree(components, (parent, leaf, out) -> out.tree2leaf = out.leaf2tree = SharedMemoryTransport.class,
				components, new Random());

		var gateway = components.getFirst();
		gateway.friendlyName = "gw";
		var ws = gateway.need(WebService.class).startHTTPServer();
		gateway.need(BlindBroadcasting.class, BFSRouting.class, ProbabilisticBroadcasting.class);

		components.forEach(
				c -> c.services(TransportService.class).forEach(t -> t.listeners.add(new TransportListener.StdOut())));

		Idawi.agenda.start();
		Idawi.agenda.stopWhen(() -> false, null);

//		Desktop.getDesktop().browse(new URI("http://localhost:8081/"));
	}
}