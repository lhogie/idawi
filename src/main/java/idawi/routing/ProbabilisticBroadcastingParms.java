package idawi.routing;

public class ProbabilisticBroadcastingParms extends RoutingParameters{
	double p = 1;

	@Override
	public long sizeOf() {
		return 8;
	}

	@Override
	public String toURLElement() {
		return ""+p;
	}

	@Override
	public void fromString(String s, RoutingService service) {
		p = Double.valueOf(s);
		
	}
	
}