package idawi.demo.website;

import java.io.IOException;

import idawi.Component;
import idawi.deploy.DeployerService;
import toools.net.SSHParms;

public class DeployToAnotherNode {
	public static void main(String[] args) throws IOException {
		var a = new Component();

		var ssh = new SSHParms();
		ssh.host = "musclotte.inria.fr";

		var ro = a.bb().exec(DeployerService.class, DeployerService.remote_deploy.class, ssh, true);

		ro.returnQ.collector().collect(10, 10, c -> System.out.println(c.messages.last().content));
	}
}
