package idawi.service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.MessageList;
import idawi.Service;
import idawi.To;

public class Virus extends Service {

	@Override
	public String getFriendlyName() {
		return "automatic deployer";
	}

	public Virus(Component peer) {
		super(peer);
		registerOperation(null, (msg, out) -> activate(out));
	}

	public void activate(Consumer<Object> feedback) {
		newThread_loop_periodic(1000, () -> {

			if (component.descriptorRegistry.size() > 0) {
				ComponentInfo c = component.descriptorRegistry.pickRandomPeer();
				To to = new To();
				to.notYetReachedExplicitRecipients = Set.of(c);
				to.service = id;
				MessageList response = send(null, to).collect();

				// the node doesn't respond
				if (response.isEmpty()) {
					for (InetAddress ip : c.inetAddresses) {
						try {
							int timeoutS = 3;

							if (ip.isReachable(timeoutS * 1000)) {
								try {
									component.lookupService(ComponentDeployer.class).deploy(Collections.singleton(c), true, timeoutS,
											false, feedback, p -> {
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
