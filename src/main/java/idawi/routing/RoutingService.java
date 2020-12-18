package idawi.routing;

import java.util.function.Consumer;

import idawi.Component;
import idawi.Operation;
import idawi.Service;
import toools.reflect.Clazz;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class RoutingService extends Service {

	public RoutingScheme scheme = new RoutingScheme_bcast(this);

	public RoutingService(Component node) {
		super(node);
		registerOperation("retrieve", (msg, out) -> out.accept(scheme));
		registerOperation("info", (msg, out) -> out.accept(scheme.toString()));
		registerOperation("set", (msg, results) -> setScheme((Class<? extends RoutingScheme>) msg.content));
	}

	@Operation
	private void listRoutes(Consumer<Object> out)
	{
		
	}
	
	@Override
	public String getFriendlyName() {
		return "routing";
	}

	public void setScheme(Class<? extends RoutingScheme> routingClass) {
		scheme = Clazz.makeInstance(routingClass);
	}
}
