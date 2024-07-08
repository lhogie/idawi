package idawi.routing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.messaging.Message;
import idawi.routing.ComponentMatcher.multicast;
import idawi.transport.TransportService;
import toools.collections.primitive.BloomFilterForLong;

public class BasicTableRoutingService extends RoutingService<RoutingParameters> implements TrafficListener {
	private final BloomFilterForLong alreadyReceivedMsgs = new BloomFilterForLong(1000);
	private final Map<Component, Component> destination_relay = new HashMap<>();

	public BasicTableRoutingService(Component node) {
		super(node);
	}

	@Override
	public void acceptImpl(Message msg, RoutingParameters parms) {
		// if the message was never received
		if (!alreadyReceivedMsgs.contains(msg.ID)) {
			alreadyReceivedMsgs.add(msg.ID);

			if (msg.qAddr.targetedComponents instanceof multicast) {
				for (var target : ((multicast) msg.qAddr.targetedComponents).target) {
					var relay = destination_relay.get(target);

					if (relay != null) {
						component.services(TransportService.class).forEach(t -> {
							if (parms.acceptTransport.test(t)) {
								var link = component.localView().g.findLink(
										l -> l.src.component.equals(component) && l.dest.component.equals(relay));

								if (link != null) {
									t.send(msg, Set.of(link), this, parms);
								}
							}
						});
					}
				}
			}
		}
	}

	@Override
	public void newMessageReceived(TransportService t, Message msg) {
		var components = msg.route.components();
		var neighbor = components.get(components.size() - 1);
		components.subList(0, components.size() - 2).forEach(c -> destination_relay.put(c, neighbor));
	}

	@Override
	public ComponentMatcher defaultMatcher(RoutingParameters parms) {
		return null;
	}

	@Override
	public String getAlgoName() {
		return "basic routing service";
	}

	@Override
	public List<RoutingParameters> dataSuggestions() {
		return null;
	}

}
