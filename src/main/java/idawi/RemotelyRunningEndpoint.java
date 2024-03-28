package idawi;

import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.routing.QueueAddress;

public class RemotelyRunningEndpoint {

	public QueueAddress destination;
//	public Class<? extends InnerClassEndpoint> endpoint;
	public MessageQueue returnQ;

	public QueueAddress getOperationInputQueueDestination() {
		var d = new QueueAddress();
		d.targetedComponents = ComponentMatcher.unicast(returnQ.service.component);
		d.queueID = destination.queueID;
		d.service = destination.service;
		return d;
	}
}
