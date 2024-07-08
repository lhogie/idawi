package idawi.routing;

public class RandomWalkData extends RoutingParameters {
	int n = 1;

	@Override
	public void fromString(String s, RoutingService service) {
		n = Integer.valueOf(s);
	}

	@Override
	public long sizeOf() {
		return 4;
	}

	@Override
	public String toURLElement() {
		return "" + n;
	}

}