package idawi.service.rest;

import idawi.Component;
import idawi.service.ServiceManager;
import idawi.service.ServiceManager.ensureStarted;

public class Rest {

	public static void main(String[] args) throws Throwable {
		Component a = new Component();
		a.lookupService(ServiceManager.class).lookupOperation(ensureStarted.class).f(RESTService.class);
		a.lookupService(RESTService.class).startHTTPServer();
	}
}