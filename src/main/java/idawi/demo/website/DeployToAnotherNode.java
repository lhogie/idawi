package idawi.demo.website;

import java.io.IOException;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.deploy.DeployerService.RemoteDeploymentRequest;

public class DeployToAnotherNode {
	public static void main(String[] args) throws IOException {
		var a = new Component("a");

		var req = new RemoteDeploymentRequest();
		req.target = a.createDigitalTwinFor("b@musclotte");
		req.ssh.host = "musclotte.inria.fr";

		var ro = a.bb().exec(DeployerService.class, DeployerService.remote_deploy.class, req);

		ro.returnQ.collector().collect(10, 10, c -> System.out.println(c.messages.last().content));
	}
}
