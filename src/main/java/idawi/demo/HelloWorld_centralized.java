package idawi.demo;

import java.net.UnknownHostException;

import idawi.Component;
import idawi.ComponentAddress;
import idawi.ComponentDescriptor;
import idawi.RegistryService;
import idawi.Service;

public class HelloWorld_centralized {
	public static void main(String[] args) throws UnknownHostException {
		int nbNodes = 300;

		for (int i = 0; i < nbNodes; ++i) {
			Component node = new Component(ComponentDescriptor.fromCDL("name=" + i));
			ComponentDescriptor next = new ComponentDescriptor();
			next.friendlyName = "" + ((i + 1) % nbNodes);
			node.lookupService(RegistryService.class).add(next);

			new Service(node) {
				{
					newThread_loop_periodic(1000,
							() -> node.lookupService(RegistryService.class).list().forEach(peer -> {
								var to = new ComponentAddress(peer).s(id).q(getFriendlyName());
								send("Hello World!", to);
							}));

					registerOperation(null, (msg, results) -> System.out.println(node.descriptor().friendlyName
							+ " just received : " + msg.content + " from " + msg.route.source()));
				}

				@Override
				public String getFriendlyName() {
					return "test";
				}

			};
		}
	}
}
