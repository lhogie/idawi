package idawi.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class ImposedRoute extends RoutingService<IRTo> {

	public final LongSet alreadyReceivedMsgs = new LongOpenHashSet();

	public ImposedRoute(Component node) {
		super(node);
	}

	@Override
	public String getAlgoName() {
		return "P2P routing";
	}
	@Override
	public String webShortcut() {
		return "ir";
	}


	@Override
	public void accept(Message msg, IRTo p) {
		if (!alreadyReceivedMsgs.contains(msg.ID)) {
			alreadyReceivedMsgs.add(msg.ID);
			var routingInfo = (IRTo) msg.currentRoutingParameters();
			var remainingRoute = routingInfo.route;

			var relay = remainingRoute == null ? null : remainingRoute.remove(0);

			for (var t : component.services(TransportService.class)) {
				
				t.send(msg, relay == null ? null : Set.of(component.localView().g.findLink(relay)), this, p);
			}
		}
	}

	@Override
	public List<IRTo> dataSuggestions() {
		var l = new ArrayList<IRTo>();
		l.add(new IRTo());
		{
			var t = new IRTo();
			t.route.addAll(component.localView().components());
			l.add(t);

		}
		return l;
	}

	@Override
	public ComponentMatcher naturalTarget(IRTo parms) {
		// TODO Auto-generated method stub
		return null;
	}
}
