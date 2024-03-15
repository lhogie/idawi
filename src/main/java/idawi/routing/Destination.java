package idawi.routing;

import java.io.Serializable;

import idawi.Service;
import toools.SizeOf;

public abstract class Destination implements Serializable, SizeOf {
	public ComponentMatcher componentMatcher;
	public MessageQDestination replyTo;
	public boolean autoStartService = false;

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

	@Override
	public long sizeOf() {
		return componentMatcher.sizeOf() + (replyTo == null ? 0 : replyTo.sizeOf());
	}

}