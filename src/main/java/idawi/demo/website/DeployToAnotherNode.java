package idawi.demo.website;

import java.io.IOException;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.deploy.DeployerService;
import idawi.deploy.DeployerService.DeploymentRequest;

public class DeployToAnotherNode {
	public static void main(String[] args) throws IOException {
		var a = new Component();
		ComponentDescriptor child = new ComponentDescriptor();
		child.name = "b";
		child.sshParameters.hostname = "musclotte.inria.fr";

		var r = new DeploymentRequest();
		r.peers.add(child);

		var ro = a.operation(DeployerService.deploy.class).execfd(r);
		
		ro.returnQ.recv_sync(10, 10, c -> System.out.println(c.messages.last().content));

//		a.lookup(DeployerService.class).deploy(Set.of(child), true, 10000, true,
//				feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));
	}
}
