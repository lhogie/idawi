package idawi.messaging;

import java.io.Serializable;

import idawi.routing.BallBroadcasting;
import idawi.routing.RoutingParameters;
import idawi.routing.RoutingService;
import toools.SizeOf;

public class RoutingStrategy implements Serializable, SizeOf {
	public Class<? extends RoutingService> routingService;
	public RoutingParameters parms;

	public RoutingStrategy(Class<? extends RoutingService> routingService, RoutingParameters parms2) {
		this.routingService = routingService;
		this.parms = parms2;
	}

	public RoutingStrategy(RoutingService r) {
		this(r.getClass(), r.defaultData());
	}

	@Override
	public long sizeOf() {
		return 4 + (parms == null ? 0 : parms.sizeOf());
	}
}