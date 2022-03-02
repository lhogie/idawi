package idawi;

public class RemotelyRunningOperation {

	public final TriggerMessage triggerMsg = new TriggerMessage();
	public MessageQueue returnQ;
	public final Service clientService;

	public RemotelyRunningOperation(Service sourceService, QueueAddress to, String operationName, MessageQueue returnQ,
			Object initialInputData) {
		this(sourceService, to, operationName, returnQ == null ? null : returnQ.addr(), initialInputData);
		this.returnQ = returnQ;
	}

	public RemotelyRunningOperation(Service clientService, QueueAddress to, String operationName,
			QueueAddress returnQaddr, Object initialInputData) {
		this.clientService = clientService;
		this.triggerMsg.operationName = operationName;
		this.triggerMsg.to = to;
		this.triggerMsg.content = initialInputData;

		if (returnQaddr != null) {
			this.triggerMsg.replyTo = returnQaddr;
		}

//		System.out.println(to + "   4" + initialInputData);

		triggerMsg.originService = clientService.getClass().getName();
		triggerMsg.send(clientService.component);
	}

	public void send(Object content) {
		var msg = new Message(content, triggerMsg.replyTo, triggerMsg.replyTo);
		msg.send(clientService.component);
	}

	public void dispose() {
		send(EOT.instance);
	}

	public String getOperationInputQueueName() {
		return triggerMsg.to.queueName;
	}
}
