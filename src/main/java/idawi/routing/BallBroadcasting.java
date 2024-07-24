package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class BallBroadcasting extends RoutingService<BallBroadcastingParms> {
	public final LongSet alreadyReceivedMsgs = new LongOpenHashSet();

	public BallBroadcasting(Component node) {
		super(node);
	}

	@Override
	public long sizeOf() {
		return 8 + alreadyReceivedMsgs.size() * 8;
	}

	@Override
	public String getAlgoName() {
		return "ball broadcasting";
	}

	@Override
	public String getFriendlyName() {
		return "ballbcast";
	}

	@Override
	public void acceptImpl(Message msg, BallBroadcastingParms parms) {
		boolean alreadyKnown = component.alreadyReceivedMsgs.contains(msg.ID)
				|| component.alreadySentMsgs.contains(msg.ID);
//		Cout.debug(msg.ID + "   " + alreadyKnown + " by " + System.identityHashCode(component));

		if (!alreadyKnown) {
			alreadyReceivedMsgs.add(msg.ID);

			for (var t : component.services(TransportService.class)) {
				if (parms.acceptTransport.test(t)) {
					if (parms.energy-- > 0) {
						t.send(msg, null, this);
					} else {
						return;
					}
				}
			}
		}
	}

	@Override
	public List<BallBroadcastingParms> dataSuggestions() {
		var l = new ArrayList<BallBroadcastingParms>();
		l.add(new BallBroadcastingParms(10));
		l.add(new BallBroadcastingParms(100));
		l.add(new BallBroadcastingParms(-1));
		return l;
	}

	@Override
	public ComponentMatcher defaultMatcher(BallBroadcastingParms parms) {
		return ComponentMatcher.all;
	}

}
