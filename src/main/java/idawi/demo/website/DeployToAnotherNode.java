package idawi.demo.website;

import java.io.IOException;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.deploy.DeployerService;
import idawi.deploy.DeployerService.DeploymentParameters;

public class DeployToAnotherNode {
	public static void main(String[] args) throws IOException {
		var a = new Component();
		ComponentDescriptor child = new ComponentDescriptor();
		child.name = "b";
		child.ssh.host = "musclotte.inria.fr";

		var r = new DeploymentParameters();
		r.targets.add(child);

		var ro = a.operation(DeployerService.deploy.class).execfd(r);
		
		ro.returnQ.collect(10, 10, c -> System.out.println(c.messages.last().content));

//		a.lookup(DeployerService.class).deploy(Set.of(child), true, 10000, true,
//				feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));
	}
}
