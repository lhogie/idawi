package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.Link;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class ForceBroadcasting extends RoutingService<ForceRoutingParms> {
	public final LongSet alreadyReceivedMsgs = new LongOpenHashSet();

	public ForceBroadcasting(Component node) {
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
	public String webShortcut() {
		return "fb";
	}

	@Override
	public void accept(Message msg, ForceRoutingParms parms) {

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
	public List<ForceRoutingParms> dataSuggestions() {
		var l = new ArrayList<ForceRoutingParms>();
		l.add(new ForceRoutingParms(10));
		l.add(new ForceRoutingParms(100));
		l.add(new ForceRoutingParms(-1));
		return l;
	}

	@Override
	public ComponentMatcher naturalTarget(ForceRoutingParms parms) {
		return ComponentMatcher.all;
	}

}
