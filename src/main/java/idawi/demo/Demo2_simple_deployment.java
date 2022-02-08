package idawi.demo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Message;
import idawi.service.DeployerService;
import idawi.service.PingService;

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

// creates a *local* peer that will drive the deployment
		Component t = new Component(ComponentDescriptor.fromCDL("name=parent"));

// describes the child peer that will be deployed to
		ComponentDescriptor child = new ComponentDescriptor();
		InetAddress childHost = InetAddress.getByName(args[0]);
		child.inetAddresses.add(childHost);
		child.name = childHost.getHostName();
		child.sshParameters.hostname = childHost.getHostName();

// deploy
		t.lookup(DeployerService.class).deploy(Set.of(child), true, 10000, true,
				feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));

// at this step the child is running on the remote host. We can interact with
// it.
		long pingTime = System.currentTimeMillis();
		Message pong = PingService.ping(t.lookup(PingService.class), child, 1000);

		if (pong == null) {
			System.err.println("ping timeout");
		} else {
			long pongDuration = System.currentTimeMillis() - pingTime;
			System.out.println("pong received after " + pongDuration + "ms");
		}
	}
}
