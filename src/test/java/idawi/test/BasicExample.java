package idawi.test;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.service.DemoService;
import idawi.service.DemoService.stringLength;

public class BasicExample {
	public static void main(String[] args) throws Throwable {
// creates a component in this JVM
		var c1 = new Component();

// prints the list of its builtin services
		c1.forEachService(s -> System.out.println(s));

// among them, picks up the service for component deployments
		var deployer = c1.service(DeployerService.class);

// and prints the operations exposed by it
		System.out.println(deployer.endpoints());

// we'll put another component in a different JVM

		var c2 = c1.service(DeployerService.class).deployInNewJVM(feedback -> System.out.println(feedback));

// asks the other component to compute something
		var l = c1.bb().exec_rpc(c2, DemoService.class, stringLength.class, "Hello Idawi!");
		System.out.println(l);

	}

}
