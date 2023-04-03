package idawi.routing;

import java.io.Serializable;
import java.util.Iterator;

import idawi.transport.TransportService;

/**
 * The route is implemented as nodes chained by links
 * 
 * @author lhogie
 *
 */

public abstract class RouteEvent implements Serializable, Iterator<RouteEvent> {
	private double date = -1;
	public TransportService transport;

	public RouteEvent( TransportService transportService) {
		this.date = transportService.component.now();
		this.transport = transportService;
	}

	public int remaining() {
		return hasNext() ? 1 + next().remaining() : 0;
	}

	@Override
	public final String toString() {
		return getClass().getName() + " (" + super.toString() + ", routing=" + routingProtocol() + ", routingParms="
				+ routingParms() + ", transport=" + transport() + ")";
	}

	public double date() {
		return date;
	}

	public abstract Class<? extends RoutingService> routingProtocol();

	public abstract RoutingData routingParms();

	public final TransportService transport() {
		return transport;
	}

}