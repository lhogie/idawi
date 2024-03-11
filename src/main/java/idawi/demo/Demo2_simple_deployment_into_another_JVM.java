package idawi.demo;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.messaging.Message;

public class Demo2_simple_deployment_into_another_JVM {
	public static void main(String[] args) throws Throwable {
// prints out the Java version
		System.out.println("You are using JDK " + System.getProperty("java.version"));

// creates a *local* peer that will drive the deployment
		var localComponent = new Component();

// deploy
		var child = localComponent.service(DeployerService.class, true).newLocalJVM();
System.out.println("new child: " + child);
// at this step the child is running on the remote host. We can interact with
// it.
		var pong = localComponent.bb().ping(child).poll_sync(3);

		if (pong == null) {
			System.err.println("ping timeout");
		} else {
			var ping = (Message) pong.content;
			double pongDuration = pong.route.last().receptionDate - ping.route.first().emissionDate;
			System.out.println("pong received after " + pongDuration + "ms");
		}
	}
}
