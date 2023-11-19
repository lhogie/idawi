package idawi.service.web;

import java.util.ArrayList;
import java.util.Random;

import idawi.Component;
import idawi.RuntimeEngine;
import idawi.routing.BlindBroadcasting;
import idawi.routing.RoutingService;
import idawi.service.DemoService;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.Topologies;

public class RunTestServer {

	public static void main(String[] args) throws Throwable {
		int n = 1;
		var components = new ArrayList<Component>();
		components.addAll(Component.createNComponent("", n));

		components.forEach(c -> new DemoService(c));

		var gateway = components.get(0);
		var ws = gateway.service(WebService.class);
		ws.startHTTPServer();

		Topologies.tree(components, (parent, leaf, out) -> out.tree2leaf = out.leaf2tree = SharedMemoryTransport.class,
				components, new Random());

		System.out.println("gw outLinks: " + gateway.outLinks());

		var c = components.get(0);
		System.out.println("pinging");
		
		RuntimeEngine.terminated = () -> false;
		RuntimeEngine.start();
		var pong = c.bb().exec(BlindBroadcasting.class, RoutingService.ping.class, null).returnQ.poll_sync();
		System.out.println("pong: " + pong);

//		Desktop.getDesktop().browse(new URI("http://localhost:8081/"));
	}
}