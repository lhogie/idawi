package idawi.test;

import java.io.IOException;

import idawi.Component;
import idawi.Service;
import idawi.deploy.DeployerService;
import idawi.deploy.DeployerService.ExtraJVMDeploymentRequest;
import idawi.knowledge_base.ComponentRef;
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
		var req = new ExtraJVMDeploymentRequest();
		req.targetDescription.ref = new ComponentRef("other component");

		c1.lookup(DeployerService.class).deployInNewJVM(req, feedback -> System.out.println(feedback));

// asks the other component to compute something
		var l = c1.bb().exec_rpc(req.targetDescription.ref, DemoService.stringLength.class, "Hello Idawi!");
		System.out.println(l);
	}
}
