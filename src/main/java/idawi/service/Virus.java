package idawi.service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.To;
import idawi.ComponentDescriptor;
import idawi.MessageList;
import idawi.RegistryService;
import idawi.Service;

public class Virus extends Service {

	@Override
	public String getFriendlyName() {
		return "automatic deployer";
	}

	public Virus(Component peer) {
		super(peer);
		registerOperation("default", (msg, out) -> activate(out));
	}

	public void activate(Consumer<Object> feedback) {
		newThread_loop_periodic(1000, () -> {

			if (!component.lookupOperation(RegistryService.list.class).list().isEmpty()) {
				ComponentDescriptor c = component.lookup(RegistryService.class).pickRandomPeer();
				var to = new To(Set.of(c)).s(Virus.class).o("default");
				MessageList response = exec(to, true, null).returnQ.collect();

				// the node doesn't respond
				if (response.isEmpty()) {
					for (InetAddress ip : c.inetAddresses) {
						try {
							int timeoutS = 3;

							if (ip.isReachable(timeoutS * 1000)) {
								try {
									component.lookup(DeployerService.class).deploy(Collections.singleton(c),
											true, timeoutS, false, feedback, p -> {
											});
									break;
								} catch (Throwable t) {
									feedback.accept(t);
								}
							}
						} catch (IOException e) {
						}
					}
				}
			}
		});
	}
}
