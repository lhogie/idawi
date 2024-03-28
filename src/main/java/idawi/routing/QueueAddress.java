package idawi.routing;

import java.io.Serializable;

import idawi.Service;
import toools.SizeOf;

public class QueueAddress implements SizeOf, Serializable {
	public ComponentMatcher targetedComponents;
	public Class<? extends Service> service;
	public String queueID;

	@Override
	public String toString() {
		return targetedComponents + "/" + service.getSimpleName() + "/" + queueID;
	}

	@Override
	public long sizeOf() {
		return 8 + 1 + SizeOf.sizeOf(queueID) + SizeOf.sizeOf(targetedComponents);
	}

}
