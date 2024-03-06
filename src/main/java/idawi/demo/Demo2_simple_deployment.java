package idawi.demo;

import java.io.IOException;
import java.util.Set;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.deploy.DeployerService.RemoteDeploymentRequest;
import idawi.messaging.Message;

/**
 * 
 * @author lhogie
 *
 * 
 *
 */

public class Demo2_simple_deployment {
	public static void main(String[] args) throws IOException {
// prints out the Java version
		System.out.println("You are using JDK " + System.getProperty("java.version"));
		System.out.println(System.getProperties());

// creates a *local* peer that will drive the deployment
		var localComponent = new Component();

// describes the child peer that will be deployed to
		var childDeployment = new RemoteDeploymentRequest();
		childDeployment.target = new Component();
		childDeployment.ssh.host = "algothe.inria.fr";

// deploy
		localComponent.service(DeployerService.class, true).deployRemotely(Set.of(childDeployment), out -> System.out.println(out),
				err -> System.err.println(err), ok -> System.out.println("peer ok: " + ok));

// at this step the child is running on the remote host. We can interact with
// it.
		var pong = localComponent.bb().ping(childDeployment.target).poll_sync(3);

		if (pong == null) {
			System.err.println("ping timeout");
		} else {
			var ping = (Message) pong.content;
			double pongDuration = pong.route.last().receptionDate - ping.route.first().emissionDate;
			System.out.println("pong received after " + pongDuration + "ms");
		}
	}
}
