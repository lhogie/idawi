package idawi.routing;

import idawi.Service;

public class MessageQDestination extends Destination { 
	public Class<? extends Service> service;
	public String queueID;
	public boolean dropIfRecipientQueueIsFull = false;

	@Override
	public Class<? extends Service> service() {
		return service;
	}

	@Override
	public String queueID() {
		return queueID;
	}

	@Override
	public String toString() {
		return "to queue " + super.toString();
	}

}
