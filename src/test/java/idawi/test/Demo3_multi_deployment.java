package idawi.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Vector;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.messaging.MessageList;
import toools.net.SSHParms;

public class Demo3_multi_deployment {

	public static void main(String[] args) throws IOException {
		// prints out the Java version
		System.out.println("You are using JDK " + System.getProperty("java.version"));

		// creates a *local* peer that will drive the deployment
		var t = new Component();

		var reqs = Arrays.stream(args).map(e -> SSHParms.fromSSHString(e)).toList();

		var children = new Vector<Component>();

		// deploy
		t.service(DeployerService.class).deployViaSSH(reqs, rsyncOut -> System.out.println("rsync: " + rsyncOut),
				rsyncErr -> System.err.println("rsync: " + rsyncErr), ok -> {
					children.add(ok);
					System.out.println("peer ok: " + ok);
				});

		long pingTime = System.currentTimeMillis();
		MessageList pongs = t.bb().ping(new HashSet<>(children)).collector().collect(1000, 1000, c -> {
		}).messages;

		if (pongs.isEmpty()) {
			System.err.println("no response");
		} else {
			long pongDuration = System.currentTimeMillis() - pingTime;
			System.out.println("pong received after " + pongDuration + "ms");
		}
	}
}