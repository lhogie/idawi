package idawi;

import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.routing.ToEndpoint;
import idawi.routing.ToQueue;

public class RemotelyRunningEndpoint {

	public ToEndpoint destination;
	public MessageQueue returnQ;

	public ToQueue getOperationInputQueueDestination() {
		var d = new ToQueue();
		d.componentMatcher = ComponentMatcher.unicast(returnQ.service.component);
		d.queueID = destination.queueID();
		d.service = destination.service();
		return d;
	}

}
