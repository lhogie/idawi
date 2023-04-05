package idawi.routing;

public class EmptyRoutingParms extends RoutingData {

	@Override
	public void fromString(String s, RoutingService service) {
		if (!s.isEmpty())
			throw new IllegalArgumentException("there are no parameter to feed");
	}

	@Override
	public long sizeOf() {
		return 0;
	}

	@Override
	public String toURLElement() {
		return "";
	}

}
