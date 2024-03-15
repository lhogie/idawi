package idawi.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import idawi.Component;
import idawi.messaging.Message;
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
			var routingInfo = (IRTo) msg.route.last().routing.parms;
			var remainingRoute = routingInfo.route;

			var relay = remainingRoute == null ? null : remainingRoute.remove(0);
			component.localView().g.findLinksConnecting(component, relay)
					.forEach(l -> l.src.send(msg, relay == null ? null : Set.of(l), this, p));
		}
	}

	@Override
	public List<IRTo> dataSuggestions() {
		var l = new ArrayList<IRTo>();
		l.add(new IRTo());
		{
			var t = new IRTo();
			t.route.addAll(Set.of(component.localView().g.pickRandomComponent(new Random()),
					component.localView().g.pickRandomComponent(new Random())));
			l.add(t);

		}
		return l;
	}

	@Override
	public ComponentMatcher naturalTarget(IRTo parms) {
		return ComponentMatcher.all;
	}
}
