package idawi.routing;

import idawi.Component;
import idawi.transport.TransportService;

public class Reception extends RouteEvent {
	Emission previousEmission;
	Emission forward;

	public Reception(Component c, TransportService transportService) {
		super(c, transportService);
	}

	public Emission previousEmission() {
		return previousEmission;
	}

	public Emission forward() {
		return forward;
	}

	@Override
	public boolean hasNext() {
		return forward != null;
	}

	@Override
	public RouteEvent next() {
		return forward;
	}

	public Reception nextReception() {
		return forward == null ? null : forward.subsequentReception();
	}

	@Override
	public Class<? extends RoutingService> routingProtocol() {
		return previousEmission().routingProtocol();
	}

	@Override
	public RoutingParms routingParms() {
		return previousEmission().routingParms();
	}
}