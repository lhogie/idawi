package idawi.demo.website;

import java.io.IOException;
import java.util.Set;

import idawi.Component;
import idawi.deploy.DeployerService;
import toools.net.SSHParms;

public class DeployToAnotherNode {
	public static void main(String[] args) throws IOException {
		var a = new Component();

		var ssh = new SSHParms();
		ssh.host = "musclotte.inria.fr";

		a.need(DeployerService.class).deployViaSSH(Set.of(ssh), fdback -> System.out.println(fdback),
				err -> System.err.println(err), ok -> System.out.println("deployed: " + ok),
				err -> err.printStackTrace());
	}
}
