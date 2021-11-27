package idawi;

public class RemotelyRunningOperation {

	public final TriggerMessage initialMsg = new TriggerMessage();
	public MessageQueue returnQ;
	public final Service clientService;

	public RemotelyRunningOperation(Service clientService, QueueAddress to, String operationName, boolean expectReturn,
			Object initialInputData) {
		this.clientService = clientService;
		this.initialMsg.operationName = operationName;
		this.initialMsg.to = to;
		this.initialMsg.content = initialInputData;

		if (expectReturn) {
			this.returnQ = clientService.createQueue("returnQ-" + clientService.returnQueueID.getAndIncrement(),
					initialMsg.to.serviceAddress.componentAddress.getNotYetReachedExplicitRecipients());
			this.initialMsg.replyTo = new ComponentAddress(clientService.component.descriptor()).s(clientService.id)
					.q(returnQ.name);
		}

		initialMsg.send(clientService.component);
	}

	public void send(Object content) {
		var msg = new Message(content, initialMsg.replyTo, initialMsg.replyTo);
		msg.send(clientService.component);
	}

	public void dispose() {
		send(EOT.instance);
	}

	public String getOperationInputQueueName() {
		return initialMsg.to.queue;
	}
}
