package idawi.demo.website;

import java.io.IOException;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.To;
import idawi.service.DeployerService;

public class DeployToAnotherNode {
	public static void main(String[] args) throws IOException {
		var a = new Component();
		ComponentDescriptor child = new ComponentDescriptor();
		child.name = "b";
		child.sshParameters.hostname = "musclotte.inria.fr";

		var ro = a.operation(DeployerService.deploy.class).exec(new To(child), true, null);
		
		ro.returnQ.recv_sync();

		a.lookup(DeployerService.class).deploy(Set.of(child), true, 10000, true,
				feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));
	}
}
