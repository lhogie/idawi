package idawi.demo.website;

import idawi.Component;
import idawi.deploy.DeployerService;

public class DeployToAnotherJVM {
	public static void main(String[] args) throws Throwable {
		// creates a component in this JVM
		var a = new Component();

		var c = a.service(DeployerService.class, true).newLocalJVM();
		System.out.println(c);
	}
}
