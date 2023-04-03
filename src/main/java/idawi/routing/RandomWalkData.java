package idawi.routing;

import toools.SizeOf;

public class RandomWalkData extends RoutingData {
	int n = 1;

	@Override
	public void fromString(String s, RoutingService service) {
		n = Integer.valueOf(s);
	}
	
	@Override
	public long sizeOf() {
		return 4;
	}

}