package idawi.demo;

import java.net.UnknownHostException;
import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Service;
import idawi.To;

public class HelloWorld_centralized {
	public static void main(String[] args) throws UnknownHostException {
		int nbNodes = 300;

		for (int i = 0; i < nbNodes; ++i) {
			Component node = new Component(ComponentInfo.fromCDL("name=" + i));
			ComponentInfo next = new ComponentInfo();
			next.friendlyName = "" + ((i + 1) % nbNodes);
			node.descriptorRegistry.add(next);

			new Service(node) {
				{
					newThread_loop_periodic(1000, () -> node.descriptorRegistry.toList().forEach(peer -> {
						To to = new To();
						to.notYetReachedExplicitRecipients = Set.of(peer);
						to.service = id;
						send("Hello World!", to, null);
					}));

					registerOperation(null, (msg, results) -> System.out.println(node.descriptor().friendlyName + " just received : "
							+ msg.content + " from " + msg.route.source()));
				}

				@Override
				public String getFriendlyName() {
					return "test";
				}

			};
		}
	}
}
