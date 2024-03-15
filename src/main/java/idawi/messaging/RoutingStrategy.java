package idawi.messaging;

import java.io.Serializable;

import idawi.routing.RoutingData;
import idawi.routing.RoutingService;
import toools.SizeOf;

public class RoutingStrategy implements Serializable, SizeOf {
	public final Class<? extends RoutingService> routingService;
	public final RoutingData parms;

	public RoutingStrategy(Class<? extends RoutingService> routingService, RoutingData parms2) {
		this.routingService = routingService;
		this.parms = parms2;
	}

	@Override
	public long sizeOf() {
		return 4 + (parms == null ? 0 : parms.sizeOf());
	}
}