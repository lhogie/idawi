package idawi.routing;

import java.io.Serializable;
import java.util.function.Predicate;

import idawi.transport.TransportService;
import toools.SizeOf;

public class RoutingParameters implements Serializable, SizeOf, URLable {
	private static final long serialVersionUID = 1L;

	public static interface SerializablePredicate<E> extends Predicate<E>, Serializable {
	}

	public SerializablePredicate<TransportService> acceptTransport = t -> true;

	public void fromString(String s, RoutingService service) {

	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + toURLElement();
	}

	@Override
	public String toURLElement() {
		return "";
	}

	@Override
	public long sizeOf() {
		return 8;
	}
}
