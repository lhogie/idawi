package idawi.service;

import java.util.function.Supplier;

import idawi.Component;
import idawi.Service;
import idawi.To;

public class PeriodicBroadcastService extends Service {
	private int nbBeaconsReceived = 0;

	public PeriodicBroadcastService(Component node) {
		super(node);
	}

	public void schedule(int periodicityMs, Supplier sendThis, Class<? extends Service> serviceID,
			String queueID) {
		newThread_loop(periodicityMs, () -> {
			To to = new To();
			to.service = serviceID;
			to.operationOrQueue = queueID;
			send(sendThis.get(), to, null);
		});
	}
	
	@Override
	public String getFriendlyName() {
		return "periodic broadcast";
	}

}
