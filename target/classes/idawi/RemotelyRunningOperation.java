package idawi;

import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.routing.MessageODestination;
import idawi.routing.MessageQDestination;

public class RemotelyRunningOperation {

	public MessageODestination destination;
	public MessageQueue returnQ;

	public MessageQDestination getOperationInputQueueDestination() {
		var d = new MessageQDestination();
		d.componentTarget = ComponentMatcher.one(returnQ.service.component);
		d.queueID = destination.queueID();
		d.service = destination.service();
		return d;
	}

}