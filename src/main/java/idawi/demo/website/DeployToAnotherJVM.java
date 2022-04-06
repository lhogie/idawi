package idawi.demo.website;

import java.io.IOException;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.service.DeployerService;

public class DeployToAnotherJVM {
	public static void main(String[] args) throws IOException {
		// creates a component in this JVM
		var a = new Component();

		// we'll put another component in a different JVM
		var bd = new ComponentDescriptor();
		bd.name = "b";
		a.lookup(DeployerService.class).deployOtherJVM(bd, true, feedback -> {}, ok -> {});
	}
}
