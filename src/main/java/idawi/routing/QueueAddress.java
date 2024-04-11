package idawi.routing;

import java.io.Serializable;

import idawi.Service;
import toools.SizeOf;

public class QueueAddress implements SizeOf, Serializable {
	public final ComponentMatcher targetedComponents;
	public final Class<? extends Service> service;
	public final String queueID;

	public QueueAddress(ComponentMatcher targetedComponents, Class<? extends Service> service, String queueID) {
		this.targetedComponents = targetedComponents;
		this.service = service;
		this.queueID = queueID;
	}

	@Override
	public String toString() {
		return targetedComponents + "/" + service.getSimpleName() + "/" + queueID;
	}

	@Override
	public long sizeOf() {
		return 8 + 1 + SizeOf.sizeOf(queueID) + SizeOf.sizeOf(targetedComponents);
	}

}
