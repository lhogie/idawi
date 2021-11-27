package idawi.service.rest;

import idawi.Component;
import idawi.service.ServiceManager;

public class Rest {

	public static void main(String[] args) throws Throwable {
		Component a = new Component();
		a.lookupService(ServiceManager.class).ensureStarted(RESTService.class);
		a.lookupService(RESTService.class).startHTTPServer();
	}
}