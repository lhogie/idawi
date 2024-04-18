package idawi;

import idawi.messaging.MessageQueue;
import idawi.routing.QueueAddress;

public class Computation {
	public final QueueAddress inputQAddr;
	public final MessageQueue returnQ;

	public Computation(QueueAddress destination, MessageQueue returnQ) {
		this.inputQAddr = destination;
		this.returnQ = returnQ;
	}
}
