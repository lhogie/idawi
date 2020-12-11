package idawi.routing;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import idawi.ComponentInfo;
import idawi.Route;
import idawi.TransportLayer;

public abstract class RoutingScheme {
	final RoutingService s;
	
	
	public RoutingScheme(RoutingService s){
		this.s= s;		
	}
	
	public abstract Collection<ComponentInfo> findRelaysToReach(TransportLayer protocol, Set<ComponentInfo> to);

	public abstract void feedWith(Route route);

	public abstract void print(Consumer<String> out);
}
