package idawi.demo;

import java.net.UnknownHostException;
import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Service;
import idawi.To;
import idawi.net.UDPDriver;
import toools.thread.Threads;

public class HelloWorld {
	public static void main(String[] args) throws UnknownHostException {
		int baseport = 4000;

		for (int i = 0; i < 200; ++i) {
			UDPDriver protocol = new UDPDriver();
			protocol.setPort(baseport + i);

			Component node = new Component(ComponentInfo.fromCDL("name=node-" + i));
			node.descriptorRegistry.addLocalPeerByPort(baseport + i + 1);

			new Service(node) {
				{
					Threads.newThread_loop_periodic(1000, () -> true, () -> {
						node.descriptorRegistry.toList().forEach(peer -> {
							To to = new To();
							to.notYetReachedExplicitRecipients = Set.of(peer);
							to.service = id;
							send("Hello World!", to, null);
						});
					});

					getQueue(null).onMsg(msg -> {
						System.out.println(
								node.descriptor() + " just received : " + msg.content + " from " + msg.route.source());
					}, () -> true);
				}

				@Override
				public String getFriendlyName() {
					return "some service";
				}

			};
		}
	}
}
