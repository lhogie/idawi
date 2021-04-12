package idawi.service;

import java.util.function.Supplier;

import idawi.Component;
import idawi.Service;
import idawi.QueueAddress;

public class GossipingService extends Service {
	private int nbBeaconsReceived = 0;

	public GossipingService(Component node) {
		super(node);
	}

	public void schedule(int periodicityMs, Supplier sendThis, Class<? extends Service> serviceID,
			String queueID) {
		newThread_loop(periodicityMs, () -> {
			QueueAddress to = new QueueAddress();
			to.service = serviceID;
			to.queue = queueID;
			send(sendThis.get(), to);
		});
	}
	
	@Override
	public String getFriendlyName() {
		return "periodic broadcast";
	}

}
