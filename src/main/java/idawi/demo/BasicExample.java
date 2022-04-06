package idawi.demo;

import java.io.IOException;
import java.util.Set;

import idawi.Component;
import idawi.To;
import idawi.deploy.DeployerService;
import idawi.ComponentDescriptor;
import idawi.Service;
import idawi.service.DemoService;

public class BasicExample {
	public static void main(String[] args) throws IOException {
// creates a component in this JVM
		var c1 = new Component();

// prints the list of its builtin services
		c1.forEachService(s -> System.out.println(s));

// among them, picks up the service for component deployments
		var deployer = c1.lookup(DeployerService.class);

// and prints the operations exposed by it
		System.out.println(deployer.lookup(Service.listOperationNames.class).f());

// we'll put another component in a different JVM
		var c2d = new ComponentDescriptor();
		c2d.name = "other component";
		c1.lookup(DeployerService.class).deployOtherJVM(c2d, true, feedback -> {
		}, ok -> {
		});

// creates a new service that asks the other component to compute something
		new Service(c1) {
			public void run() {
				// executes an operation (exposed by DummyService) which computes the length of
				// a given string
				var l = execf(new To(Set.of(c2d)).o(DemoService.stringLength.class), 1, 1, "Hello Idawi!");
				System.out.println(l);
			}
		}.run();
	}
}
