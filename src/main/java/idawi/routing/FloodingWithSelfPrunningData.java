package idawi.routing;

import java.util.Collection;

import idawi.Component;

public class FloodingWithSelfPrunningData extends RoutingParameters {
	Collection<Component> outNeighbors;

	// by default use all transports available
//	Predicate<TransportService> useTransport = t -> true;
}