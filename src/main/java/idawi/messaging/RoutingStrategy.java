package idawi.messaging;

import java.io.Serializable;

import idawi.routing.RoutingParameters;
import idawi.routing.RoutingService;
import toools.SizeOf;

public class RoutingStrategy implements Serializable, SizeOf {
	public final Class<? extends RoutingService> routingService;
	public final RoutingParameters parms;

	public RoutingStrategy(RoutingService routingService, RoutingParameters parms2) {
		this.routingService = routingService.getClass();
		this.parms = parms2;
	}

	@Override
	public long sizeOf() {
		return 4 + (parms == null ? 0 : parms.sizeOf());
	}
}