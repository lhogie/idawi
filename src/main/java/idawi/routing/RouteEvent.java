package idawi.routing;

import java.io.Serializable;
import java.util.Iterator;

import idawi.Component;
import idawi.knowledge_base.ComponentRef;
import idawi.transport.TransportService;

/**
 * The route is implemented as nodes chained by links
 * 
 * @author lhogie
 *
 */

public abstract class RouteEvent implements Serializable, Iterator<RouteEvent> {
	public ComponentRef component;
	private double date = -1;
	private Class<? extends TransportService> transport;

	public RouteEvent(Component c, TransportService transportService) {
		this.component = c.ref();
		this.date = c.now();
		this.transport = transportService.getClass();
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

	public abstract RoutingParms routingParms();

	public final Class<? extends TransportService> transport() {
		return transport;
	}


}