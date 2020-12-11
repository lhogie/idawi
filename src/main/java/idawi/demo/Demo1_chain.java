package idawi.demo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;

import idawi.ComponentInfo;
import idawi.Message;
import idawi.To;
import idawi.net.IPDriver;
import idawi.net.UDPDriver;
import toools.thread.Threads;

/**
 * 
 * @author lhogie
 *
 * 
 *
 */

public class Demo1_chain {
	public static void main(String[] args) throws UnknownHostException {
		final int basePort = 4000;

		for (int port = basePort; port < basePort + 20; ++port) {
			ComponentInfo me = new ComponentInfo();
			me.friendlyName = "node-" + port;
			me.inetAddresses.add(InetAddress.getLoopbackAddress());
			me.tcpPort = port;

			IPDriver network = new UDPDriver();
			network.setPort(port);

			// what to do when a new message arrives
			network.setNewMessageConsumer(msg -> System.out.println(me + "> just received from " + msg.route.source()
					+ " via protocol " + msg.route.last().protocolName + ": " + msg.content));

			ComponentInfo next = new ComponentInfo();
			next.friendlyName = "node-" + (port + 1);
			next.inetAddresses.add(me.inetAddresses.get(0)); // same host as me
			next.udpPort = next.tcpPort = port + 1;

			// every second, says hello to the next peer
			Threads.newThread_loop_periodic(1000, () -> true, () -> {
				Message msg = new Message();
				msg.to = new To();
				msg.to.notYetReachedExplicitRecipients = Set.of(next);
				msg.content = "Hello World!";
				msg.route.add(me);
				network.send(msg, Arrays.asList(next));
			});
		}
	}
}
