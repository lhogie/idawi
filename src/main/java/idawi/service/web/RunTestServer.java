package idawi.service.web;

import java.util.ArrayList;
import java.util.Random;

import idawi.Component;
import idawi.Idawi;
import idawi.service.DemoService;
import idawi.service.ServiceManager;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.Topologies;

public class RunTestServer {

	public static void main(String[] args) throws Throwable {
		int n = 5;

		var components = new ArrayList<Component>();
		components.addAll(Component.createNComponent(n));
		components.forEach(c -> new DemoService(c));
		components.forEach(c -> new ServiceManager(c));

		Topologies.tree(components, (parent, leaf, out) -> out.tree2leaf = out.leaf2tree = SharedMemoryTransport.class,
				components, new Random());

		for (int i = 0; i < n; ++i) {
			components.add(new Component("Component" + i));

		}

		var gateway = components.getFirst();
		var ws = gateway.service(WebService.class, true);
		ws.startHTTPServer();

		Idawi.agenda.setTerminationCondition(() -> false);
		Idawi.agenda.start();
		Idawi.agenda.waitForCompletion();

//		Desktop.getDesktop().browse(new URI("http://localhost:8081/"));
	}
}