package idawi.routing;

import java.util.Collection;
import java.util.function.Predicate;

import idawi.Component;
import idawi.transport.TransportService;

public class SPPParm extends EmptyRoutingParms {
	Collection<Component> neighbors;
	
	// by default use all transports available
//	Predicate<TransportService> useTransport = t -> true;
}