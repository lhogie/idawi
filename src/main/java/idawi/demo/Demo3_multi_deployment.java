package idawi.demo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.MessageList;
import idawi.service.DeployerService;
import idawi.service.PingService;

public class Demo3_multi_deployment {

	public static void main(String[] args) throws IOException {
		// prints out the Java version
		System.out.println("You are using JDK " + System.getProperty("java.version"));

		// creates a *local* peer that will drive the deployment
		Component t = new Component(ComponentDescriptor.fromCDL("name=parent"));

		Set<ComponentDescriptor> children = new HashSet<ComponentDescriptor>();
		for (int i = 0; i < args.length; i++) {
			// send the child peer that will be deployed to
			ComponentDescriptor p = new ComponentDescriptor();
			InetAddress childHost = InetAddress.getByName(args[i]);

			p.sshParameters.username = "alacomme";
			p.inetAddresses.add(childHost);
			p.name = childHost.getHostName();
			p.sshParameters.hostname = childHost.getHostName();

			children.add(p);
		}

		// deploy
		t.lookup(DeployerService.class).deploy(children, true, 10000, true,
				feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));

		long pingTime = System.currentTimeMillis();
		MessageList pongs = t.lookup(PingService.class).ping(children).recv_sync(1000, 1000, c -> {
		}).messages;

		if (pongs.isEmpty()) {
			System.err.println("no response");
		} else {
			long pongDuration = System.currentTimeMillis() - pingTime;
			System.out.println("pong received after " + pongDuration + "ms");
		}
	}
}