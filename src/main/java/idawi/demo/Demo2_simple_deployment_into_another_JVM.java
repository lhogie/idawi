package idawi.demo;

import java.io.IOException;
import java.util.Set;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.deploy.DeployerService.ExtraJVMDeploymentRequest;
import idawi.messaging.Message;

public class Demo2_simple_deployment_into_another_JVM {
	public static void main(String[] args) throws IOException {
// prints out the Java version
		System.out.println("You are using JDK " + System.getProperty("java.version"));

// creates a *local* peer that will drive the deployment
		var localComponent = new Component();

// describes the child peer that will be deployed to
		var childDeployment = new ExtraJVMDeploymentRequest();
		childDeployment.target = new Component();

// deploy
		localComponent.service(DeployerService.class).deployInNewJVMs(Set.of(childDeployment),
				s -> System.out.println(s), ok -> System.out.println(ok));

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
