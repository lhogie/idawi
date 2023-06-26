package idawi.routing;

import toools.io.Utilities;

public class ForceRoutingParms extends RoutingData {
	int force;

	public ForceRoutingParms(int initialForce) {
		this.force = initialForce;
	}

	public ForceRoutingParms() {
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