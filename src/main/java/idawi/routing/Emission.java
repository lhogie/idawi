package idawi.routing;

import idawi.transport.TransportService;

public class Emission extends RouteEvent {
	Reception previousReception;
	Reception subsequentReception;
	private Class<? extends RoutingService> routingProtocol;
	private RoutingParms routingParms;

	public Emission(RoutingService routingProtocol, RoutingParms routingParms, TransportService t) {
		super(routingProtocol.component, t);
		this.routingProtocol = routingProtocol.getClass();
		this.routingParms = routingParms;
	}

	@Override
	public Class<? extends RoutingService> routingProtocol() {
		return routingProtocol;
	}

	@Override
	public RoutingParms routingParms() {
		return routingParms;
	}

	public Emission nextEmission() {
		return subsequentReception == null ? null : subsequentReception.forward();
	}

	public Reception previousReception() {
		return previousReception;
	}

	public Reception subsequentReception() {
		return subsequentReception;
	}

	@Override
	public boolean hasNext() {
		return subsequentReception != null;
	}

	@Override
	public Reception next() {
		return subsequentReception;
	}

	public boolean equals(Object o) {
		var e = (Emission) o;
		return routingProtocol.equals(e.routingProtocol) && routingParms.equals(e.routingParms);
	}




}