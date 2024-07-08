package idawi.demo;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.messaging.Message;
import idawi.service.local_view.LocalViewService;

public class Demo2_simple_deployment_into_another_JVM {
	public static void main(String[] args) throws Throwable {
// prints out the Java version
		System.out.println("You are using JDK " + System.getProperty("java.version"));

// creates a *local* peer that will drive the deployment
		var localComponent = new Component();
		localComponent.need(LocalViewService.class);

// deploy
		var child = localComponent.need(DeployerService.class).newLocalJVM("baby");
		System.out.println("new child: " + child);
// at this step the child is running on the remote host. We can interact with
// it.
		System.out.println(localComponent.localView().g);
		
		
		var pong = (Message) localComponent.defaultRoutingProtocol().ping(child).content;
		var route = pong.route;
		double pongDuration = pong.route.getLast().receptionDate - route.first().emissionDate;
		System.out.println("pong received after " + pongDuration + "ms");
	}
}
