package idawi.routing;

import java.io.Serializable;
import java.util.Iterator;

import idawi.transport.TransportService;
import toools.text.json.JSONElement;
import toools.text.json.JSONMap;
import toools.text.json.JSONable;

/**
 * The route is implemented as nodes chained by links
 * 
 * @author lhogie
 *
 */

public abstract class RouteEvent implements Serializable, Iterator<RouteEvent>, JSONable {
	private double date = -1;
	public TransportService transport;

	public RouteEvent(TransportService transportService) {
		this.date = transportService.component.now();
		this.transport = transportService;
	}

	public int remaining() {
		return hasNext() ? 1 + next().remaining() : 0;
	}

	@Override
	public final String toString() {
		// return toJSONElement().toString();
		return getClass().getName() + " (" + super.toString() + ", routing=" + routingProtocol() + ", routingParms="
				+ routingParms() + ", transport=" + transport() + ")";
	}

	@Override
	public JSONElement toJSONElement() {
		var e = new JSONMap();
		e.add("name", getClass().getName());
		e.add("routing", routingProtocol());
		e.add("routingParms", routingParms());
		e.add("transport", transport());
		return e;
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