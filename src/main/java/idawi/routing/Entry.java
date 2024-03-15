package idawi.routing;

import java.io.Serializable;

import idawi.messaging.RoutingStrategy;
import idawi.transport.Link;
import toools.SizeOf;

public class Entry implements Serializable, SizeOf {
	public Link link;
	public double emissionDate, receptionDate;
	public RoutingStrategy routing;
	
	
	public Entry(Link l, Class<? extends RoutingService> routing) {
		this.link = l;
		this.routing = new RoutingStrategy(routing, null);
	}

	public double duration() {
		return receptionDate - emissionDate;
	}



	@Override
	public String toString() {
		return link.toString();
	}


	@Override
	public long sizeOf() {
		return 8 + 16  + routing.sizeOf();
	}
}