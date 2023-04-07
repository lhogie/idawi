package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.OutNeighbor;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import toools.io.Cout;

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
			Cout.debug(component +  " runs FB routing "+parms.force);

			for (var t : component.services(TransportService.class)) {
				var recipients = new ArrayList<OutNeighbor>();

				for (var c : t.neighborhood()) {
					// if the message is still powerful enough
					if (parms.force-- >= 1) {
						recipients.add(c);
					} else {
						return;
					}
				}

				Cout.debug("multicast " + recipients);
				t.multicast(msg, recipients, this, parms);
			}
		}
	}


	
	@Override
	public List<ForceRoutingParms> dataSuggestions() {
		var l = new ArrayList<ForceRoutingParms>();
		l.add(new ForceRoutingParms(10));
		l.add(new ForceRoutingParms(100));
		l.add(new ForceRoutingParms(10000));
		return l;
	}

	@Override
	public ComponentMatcher naturalTarget(ForceRoutingParms parms) {
		return ComponentMatcher.all;
	}

}
