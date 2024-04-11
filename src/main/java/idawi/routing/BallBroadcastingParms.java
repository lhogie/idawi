package idawi.routing;

import toools.io.Utilities;

public class BallBroadcastingParms extends RoutingParameters {
	public int energy;

	public BallBroadcastingParms(int initialEnergy) {
		this.energy = initialEnergy;
	}

	public BallBroadcastingParms() {
		this(100);
	}

	@Override
	public void fromString(String s, RoutingService r) {
		var m = Utilities.csv2map(s);
		this.energy = Integer.valueOf(m.get("force"));
	}

	@Override
	public long sizeOf() {
		return 8;
	}

	@Override
	public String toURLElement() {
		return "" + energy;
	}

}