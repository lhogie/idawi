package idawi.service;

import java.util.function.Supplier;

import idawi.Component;
import idawi.ComponentAddress;
import idawi.Service;

public class GossipingService extends Service {
	private int nbBeaconsReceived = 0;

	public GossipingService(Component node) {
		super(node);
	}

	public void schedule(int periodicityMs, Supplier sendThis, Class<? extends Service> serviceID, String queueID) {
		var to = new ComponentAddress().s(serviceID).q(queueID);
		newThread_loop(periodicityMs, () -> send(sendThis.get(), to));
	}

	@Override
	public String getFriendlyName() {
		return "periodic broadcast";
	}

}
