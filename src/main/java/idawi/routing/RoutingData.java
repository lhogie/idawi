package idawi.routing;

import java.io.Serializable;

import toools.SizeOf;

public abstract class RoutingData implements Serializable, SizeOf, URLable {
	private static final long serialVersionUID = 1L;

	public abstract void fromString(String s, RoutingService service);

	@Override
	public String toString() {
		return getClass().getName() + ": " + toURLElement();
	}
}
