package idawi.service.rest;

import java.awt.Desktop;
import java.net.URI;

import idawi.Component;
import idawi.net.LMI;
import idawi.service.ServiceManager;

public class RunTestServer {

	public static void main(String[] args) throws Throwable {
		Component a = new Component("a");
		Component b = new Component("b");
		Component c = new Component("c");
		LMI.connect(a, b);
		LMI.connect(b, c);
		a.operation(ServiceManager.ensureStarted.class).f(WebServer.class);
		a.lookup(WebServer.class).startHTTPServer();
		Desktop.getDesktop().browse(new URI("http://localhost:8081/"));
	}
}