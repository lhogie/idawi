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

public class RunTestServer {

	public static void main(String[] args) throws Throwable {
		int n = 2;
		var components = new ArrayList<Component>();
		components.addAll(Component.createNComponent(n));

		for (int i = 0; i < n; ++i) {
			var c = components.get(i);
			c.friendlyName = "c" + i;
			new Loopback(c);
			new DemoService(c);
			new ServiceManager(c);
			new PingService(c);
		}

		Topologies.tree(components, (parent, leaf, out) -> out.tree2leaf = out.leaf2tree = SharedMemoryTransport.class,
				components, new Random());

		var gateway = components.getFirst();
		gateway.friendlyName = "gw";
		var ws = gateway.need(WebService.class).startHTTPServer();
		gateway.need(BlindBroadcasting.class);
		gateway.need(BFSRouting.class);
		gateway.need(ProbabilisticBroadcasting.class);

		Idawi.agenda.start();
		Idawi.agenda.stopWhen(() -> false, null);

//		Desktop.getDesktop().browse(new URI("http://localhost:8081/"));
	}
}