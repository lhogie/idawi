package idawi.service.web;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;

import idawi.Component;
import idawi.knowledge_base.DigitalTwinService;
import idawi.routing.BlindBroadcasting;
import idawi.transport.SharedMemoryTransport;

public class RunTestServer {

	public static void main(String[] args) throws Throwable {
		var components = new ArrayList<Component>();

		for (var n : "abcde".toCharArray()) {
			var a = new Component("" + n);
			new SharedMemoryTransport(a);
			new DigitalTwinService(a);
			new BlindBroadcasting(a);
			components.add(a);
		}

		var gateway = components.get(0);
		var ws = new WebService(gateway);
		ws.startHTTPServer();

		
		SharedMemoryTransport.randomTree(components, SharedMemoryTransport.class);

		Desktop.getDesktop().browse(new URI("http://localhost:8081/"));
	}
}