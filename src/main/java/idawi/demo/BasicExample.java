package idawi.demo;

import java.io.IOException;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Service;
import idawi.ServiceAddress;
import idawi.service.DeployerService;
import idawi.service.DummyService;

public class BasicExample {
	public static void main(String[] args) throws IOException {
// creates a component in this JVM
		var c1 = new Component();

// prints the list of its builtin services
		c1.services().forEach(s -> System.out.println(s));

// among them, picks up the service for component deployments
		var deployer = c1.lookupService(DeployerService.class);

// and prints the operations exposed by it
		System.out.println(deployer.listOperationNames());

// we'll put another component in a different JVM
		var c2d = new ComponentDescriptor();
		c2d.friendlyName = "other component";
		c1.lookupService(DeployerService.class).deployOtherJVM(c2d, true, feedback -> {
		}, ok -> {
		});

// creates a new service that asks the other component to compute something
		new Service(c1) {
			public void run() {
				// executes an operation (exposed by DummyService) which computes the length of
				// a given string
				var l = exec(new ServiceAddress(Set.of(c2d), DummyService.class), DummyService.stringLength2, 1, 1,
						"Hello Idawi!");
				System.out.println(l);
			}
		}.run();
	}
}
