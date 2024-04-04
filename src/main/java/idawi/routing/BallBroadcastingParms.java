package idawi.routing;

import toools.io.Utilities;

public class BallBroadcastingParms extends RoutingParameters {
	int force;

	public BallBroadcastingParms(int initialForce) {
		this.force = initialForce;
	}

	public BallBroadcastingParms() {
		this(100);
	}

	@Override
	public void fromString(String s, RoutingService r) {
		var m = Utilities.csv2map(s);
		this.force = Integer.valueOf(m.get("force"));
	}

	@Override
	public long sizeOf() {
		return 8 * 3;
	}

	@Override
	public String toURLElement() {
		return force < 0 ? "+inf" : "" + force;
	}

}