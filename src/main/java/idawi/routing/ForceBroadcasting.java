package idawi.routing;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.messaging.Message;
import idawi.transport.OutNeighbor;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class ForceBroadcasting extends RoutingService<EvaporatingRoutingjavaParm> {
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
		return "division broadcasting";
	}
	
	@Override
	public String webShortcut() {
		return "fb";
	}

	@Override
	public void accept(Message msg, EvaporatingRoutingjavaParm parms) {
		// the message was never received
		if (!alreadyReceivedMsgs.contains(msg.ID)) {
			alreadyReceivedMsgs.add(msg.ID);

			for (var t : component.services(TransportService.class)) {
				var recipient = new ArrayList<OutNeighbor>();

				for (var c : t.neighborhood()) {
					// if the message is still powerful enough
					if (parms.force-- >= 1) {
						recipient.add(c);
					} else {
						return;
					}
				}

				t.multicast(msg, recipient, this, parms);
			}
		}
	}


	
	@Override
	public List<EvaporatingRoutingjavaParm> dataSuggestions() {
		var l = new ArrayList<EvaporatingRoutingjavaParm>();
		l.add(new EvaporatingRoutingjavaParm(1));
		l.add(new EvaporatingRoutingjavaParm(10));
		l.add(new EvaporatingRoutingjavaParm(100));
		return l;
	}

	@Override
	public TargetComponents naturalTarget(EvaporatingRoutingjavaParm parms) {
		return TargetComponents.all;
	}

}
