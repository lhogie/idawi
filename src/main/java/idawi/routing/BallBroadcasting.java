package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.Link;
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
		return "force broadcasting";
	}

	@Override
	public String getFriendlyName() {
		return "fb";
	}

	@Override
	public void acceptImpl(Message msg, BallBroadcastingParms parms) {

		// the message was never received
		if (!alreadyReceivedMsgs.contains(msg.ID)) {
			alreadyReceivedMsgs.add(msg.ID);

			for (var t : component.services(TransportService.class)) {
				var recipients = new ArrayList<Link>();

				t.activeOutLinks().forEach(l -> {
					// if the message is still powerful enough
					if (parms.force-- >= 1) {
						recipients.add(l);
					} else {
						return;
					}
				});

				t.send(msg, recipients, this, parms);
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
