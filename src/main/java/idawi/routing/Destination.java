package idawi.routing;

import java.io.Serializable;

import idawi.Service;

public abstract class Destination implements Serializable {
	public ComponentMatcher componentMatcher;
	public MessageQDestination replyTo;

	public abstract Class<? extends Service> service();

	public abstract String queueID();

	@Override
	public String toString() {
		var s = componentMatcher + "/" + service().getSimpleName() + "/" + queueID();

		if (replyTo != null) {
			s += ", replyTo: " + replyTo;
		}

		return s;
	}
}