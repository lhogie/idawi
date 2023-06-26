package idawi.service.web;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;

import idawi.Component;
import idawi.service.DemoService;
import idawi.transport.SharedMemoryTransport;

public class RunTestServer {

	public static void main(String[] args) throws Throwable {
		int n = 0;
		var components = new ArrayList<Component>();
		components.addAll(Component.createNComponent("", n));

		components.forEach(c -> new DemoService(c));

		var gateway = components.get(0);
		var ws = gateway.need(WebService.class);
		ws.startHTTPServer();

		SharedMemoryTransport.randomTree(components, SharedMemoryTransport.class);
		System.out.println("gw reaches: " + gateway.outLinks());

		Desktop.getDesktop().browse(new URI("http://localhost:8081/"));
	}
}