package idawi;

import java.util.Set;

public class RemotelyRunningOperation {

	public final ExecMessage initialMsg = new ExecMessage();
	public MessageQueue returnQ;
	public final Service clientService;

	public RemotelyRunningOperation(Service client, QueueAddress to, String operationName, boolean expectReturn,
			Object initialInputData) {
		this.clientService = client;
		this.initialMsg.operationName = operationName;
		this.initialMsg.to = to;
		this.initialMsg.content = initialInputData;

		if (expectReturn) {
			this.returnQ = client.createQueue("q" + client.returnQueueID.getAndIncrement(),
					initialMsg.to.getNotYetReachedExplicitRecipients());
			this.initialMsg.requester = QueueAddress.to(Set.of(client.component.descriptor()), client.id, returnQ.name);
		}

		initialMsg.send(client.component);
	}

	public void send(Object content) {
		var msg = new Message(content, initialMsg.requester, initialMsg.requester);
		msg.send(clientService.component);
	}

	public void dispose() {
		send(EOT.instance);
	}

	public String getOperationInputQueueName() {
		return initialMsg.to.queue;
	}
}
