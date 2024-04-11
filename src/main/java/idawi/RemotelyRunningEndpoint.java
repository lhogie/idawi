package idawi;

import idawi.messaging.MessageQueue;
import idawi.routing.QueueAddress;

public class RemotelyRunningEndpoint {
	public final QueueAddress inputQAddr;
	public final MessageQueue returnQ;

	public RemotelyRunningEndpoint(QueueAddress destination, MessageQueue returnQ) {
		this.inputQAddr = destination;
		this.returnQ = returnQ;
	}
}
