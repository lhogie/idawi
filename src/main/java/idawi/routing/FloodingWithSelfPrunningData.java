package idawi.routing;

import java.util.Collection;
import java.util.function.Predicate;

import idawi.Component;
import idawi.transport.TransportService;

public class FloodingWithSelfPrunningData extends EmptyRoutingParms {
	Collection<Component> outNeighbors;
	
	// by default use all transports available
//	Predicate<TransportService> useTransport = t -> true;
}