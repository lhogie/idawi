package idawi.demo;

import java.io.IOException;
import java.util.Set;
import java.util.Vector;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.messaging.Message;
import idawi.service.PingService;
import toools.net.SSHParms;

/**
 * 
 * @author lhogie
 *
 * 
 *
 */

public class Demo2_simple_deployment {
	public static void main(String[] args) throws IOException {
// creates a *local* peer that will drive the deployment
		var localComponent = new Component();
		localComponent.friendlyName = "local";

// describes the child peer that will be deployed to
		var childDeployment = SSHParms.fromSSHString("algothe.inria.fr");

// deploy
		var v = new Vector<Component>();

		localComponent.service(DeployerService.class, true).deployViaSSH(Set.of(childDeployment),
				out -> System.out.println(out), err -> System.err.println(err), ok -> v.add(ok),
				err -> err.printStackTrace());

// at this step the child is running on the remote host. We can interact with
// it.
		var pong = localComponent.defaultRoutingProtocol().ping(v.getFirst());

		if (pong == null) {
			System.err.println("ping timeout");
		} else {
			var ping = (Message) pong.content;
			double pongDuration = pong.route.getLast().receptionDate - ping.route.first().emissionDate;
			System.out.println("pong received after " + pongDuration + "ms");
		}
	}
}
