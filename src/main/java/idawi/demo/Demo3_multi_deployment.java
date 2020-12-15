package idawi.demo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.MessageList;
import idawi.service.ComponentDeployer;
import idawi.service.PingPong;

public class Demo3_multi_deployment {

	public static void main(String[] args) throws IOException {
		// prints out the Java version
		System.out.println("You are using JDK " + System.getProperty("java.version"));

		// creates a *local* peer that will drive the deployment
		Component t = new Component(ComponentInfo.fromCDL("name=parent"));

		Set<ComponentInfo> children = new HashSet<ComponentInfo>();
		for (int i = 0; i < args.length; i++) {
			// send the child peer that will be deployed to
			ComponentInfo p = new ComponentInfo();
			InetAddress childHost = InetAddress.getByName(args[i]);

			p.sshParameters.username = "alacomme";
			p.inetAddresses.add(childHost);
			p.friendlyName = childHost.getHostName();
			p.sshParameters.hostname = childHost.getHostName();

			children.add(p);
		}

		// deploy
		t.lookupService(ComponentDeployer.class).deploy(children, true, 10000, true,
				feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));

		long pingTime = System.currentTimeMillis();
		MessageList pongs = t.lookupService(PingPong.class).ping(children, 1000);

		if (pongs.isEmpty()) {
			System.err.println("no response");
		} else {
			long pongDuration = System.currentTimeMillis() - pingTime;
			System.out.println("pong received after " + pongDuration + "ms");
		}
	}
}