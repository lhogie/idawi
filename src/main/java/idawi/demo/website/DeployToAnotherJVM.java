package idawi.demo.website;

import java.io.IOException;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.deploy.DeployerService.ExtraJVMDeploymentRequest;
import idawi.knowledge_base.ComponentRef;

public class DeployToAnotherJVM {
	public static void main(String[] args) throws IOException {
		// creates a component in this JVM
		var a = new Component();

		// we'll put another component in a different JVM
		var req = new ExtraJVMDeploymentRequest();
		req.targetDescription.ref = new ComponentRef("b");

		a.lookup(DeployerService.class).deployInNewJVM(req, feedback -> System.out.println(feedback));
	}
}
