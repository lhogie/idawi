package idawi.service;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.ComponentAddress;
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
		registerOperation(null, (msg, out) -> activate(out));
	}

	public void activate(Consumer<Object> feedback) {
		newThread_loop_periodic(1000, () -> {

			if (component.lookupService(RegistryService.class).list().size() > 0) {
				ComponentDescriptor c = component.lookupService(RegistryService.class).pickRandomPeer();
				var to = new ComponentAddress(Set.of(c));
				MessageList response = exec(to, new OperationID(id, null), true, null).returnQ.collect();

				// the node doesn't respond
				if (response.isEmpty()) {
					for (InetAddress ip : c.inetAddresses) {
						try {
							int timeoutS = 3;

							if (ip.isReachable(timeoutS * 1000)) {
								try {
									component.lookupService(DeployerService.class).deploy(Collections.singleton(c),
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
