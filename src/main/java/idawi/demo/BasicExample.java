package idawi.demo;

import idawi.Component;
import idawi.Service;
import idawi.ServiceAddress;
import idawi.service.DummyService;

public class BasicExample {
	public static void main(String[] args) {
		// creates a new component
		var c = new Component();
		
		// prints the list of builtin services
		c.services().forEach(s -> System.out.println(s));

		// among them, picks up the dummy service, which is there only for demo and test purposes
		var dummyService = c.lookupService(DummyService.class);
		
		// and print the operations exposed by it
		System.out.println(dummyService.listOperationNames());
		
		// creates a new service to show how to execute code in Idawi
		new Service(c) {
			public void run() {
				// executes an operation (exposed by DummyService) which computes the length of a given string
				var l = exec(dummyService.getAddress(), DummyService.stringLength_parameterized, 1, 1, "Hello Idawi!");
				System.out.println(l);
			}
		}.run();
	}
}
