package idawi.routing;

public class EmptyRoutingParms extends RoutingParameters {

	@Override
	public void fromString(String s, RoutingService service) {
		if (!s.isEmpty())
			throw new IllegalArgumentException("this routing protocol accepts no parameter");
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
