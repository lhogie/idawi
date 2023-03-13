package idawi.routing;

import java.io.Serializable;

import idawi.Service;

public abstract class Destination implements Serializable {

	public TargetComponents componentTarget;

	public abstract Class<? extends Service> service();

	public abstract String queueID();
	public MessageQDestination replyTo;

	@Override
	public String toString() {
		return componentTarget + "/" + service().getName() + "." + queueID()  + ", replyTo: " + replyTo;
	}
}