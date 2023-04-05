package idawi.routing;

import toools.io.Utilities;

public class EvaporatingRoutingjavaParm extends RoutingData {
	int force;

	public EvaporatingRoutingjavaParm(int initialForce) {
		this.force = initialForce;
	}

	public EvaporatingRoutingjavaParm() {
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
		return "" + force;
	}

}