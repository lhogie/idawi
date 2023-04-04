package idawi.service.web;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;

import idawi.Component;
import idawi.transport.SharedMemoryTransport;

public class RunTestServer {

	public static void main(String[] args) throws Throwable {
		int n = 1;
		var components = new ArrayList<Component>();
		components.add(new Component("gw"));

		for (int i = 0; i < 2; ++i) {
			components.add(new Component("" + i));
		}

		var gateway = components.get(0);
		var ws = new WebService(gateway);
		ws.startHTTPServer();

		SharedMemoryTransport.randomTree(components, SharedMemoryTransport.class);
		System.out.println("gw reaches: " + gateway.neighbors());

		 Desktop.getDesktop().browse(new URI("http://localhost:8081/"));
	}
}