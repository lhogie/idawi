package idawi.demo.website;

import java.io.IOException;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.service.DeployerService;

public class DeployToAnotherNode {
	public static void main(String[] args) throws IOException {
		var a = new Component();
		ComponentDescriptor child = new ComponentDescriptor();
		child.name = "b";
		child.sshParameters.hostname = "192.168.32.44";
		a.lookup(DeployerService.class).deploy(Set.of(child), true, 10000, true,
				feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));
	}
}
