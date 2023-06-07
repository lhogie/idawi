package idawi.routing;

import java.io.Serializable;

import toools.SizeOf;

public abstract class RoutingData implements Serializable, SizeOf {
	private static final long serialVersionUID = 1L;

	public abstract void fromString(String s, RoutingService service);

	public abstract String toURLElement();

	@Override
	public String toString() {
		return getClass().getName() + ": " + toURLElement();
	}
}
