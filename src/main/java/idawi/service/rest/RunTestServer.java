package idawi.service.rest;

import java.awt.Desktop;
import java.net.URI;

import idawi.Component;
import idawi.knowledge_base.ComponentRef;
import idawi.service.ServiceManager;
import idawi.transport.SharedMemoryTransport;

public class RunTestServer {

	public static void main(String[] args) throws Throwable {
		Component a = new Component(new ComponentRef("a"));
		Component b = new Component(new ComponentRef("b"));
		Component c = new Component(new ComponentRef("c"));
		a.lookup(SharedMemoryTransport.class).connectTo(b);
		b.lookup(SharedMemoryTransport.class).connectTo(c);
		a.operation(ServiceManager.ensureStarted.class).f(WebService.class);
		a.lookup(WebService.class).startHTTPServer();
		Desktop.getDesktop().browse(new URI("http://localhost:8081/"));
	}
}