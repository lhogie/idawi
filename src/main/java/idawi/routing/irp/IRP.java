package idawi.routing.irp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import idawi.Component;
import idawi.messaging.Message;
import idawi.routing.ComponentMatcher;
import idawi.routing.RoutingService;
import idawi.service.local_view.DigitalTwinListener;
import idawi.transport.Link;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class IRP extends RoutingService<IRPParms> {
	public final LongSet alreadyReceivedMsgs = new LongOpenHashSet();
	public final ConcurrentHashMap<Long, Message> aliveMessages = new ConcurrentHashMap<>();

	public IRP(Component node) {
		super(node);

		component.localView().listeners.add(new DigitalTwinListener() {

			@Override
			public void newComponent(Component p) {
			}

			@Override
			public void componentHasGone(Component a) {
			}

			@Override
			public void linkActivated(Link l) {
				if (component.equals(l.src.component)) {
					var tt = component.service(l.src.getClass());

					synchronized (aliveMessages) {
						for (Message msg : aliveMessages.values()) {
							tt.send(msg, Set.of(l), IRP.this, msg.route.last().routing.parms);
						}
					}
				}
			}

			@Override
			public void linkDeactivated(Link l) {
			}
		});
	}

	@Override
	public void accept(Message msg, IRPParms p) {
		// the message was never received
		if (!alreadyReceivedMsgs.contains(msg.ID)) {
			alreadyReceivedMsgs.add(msg.ID);

			boolean wentToFar = p.coverage > msg.route.len() / 2;
			boolean outdated = msg.route.isEmpty() ? false : p.validityDuration > msg.route.duration();

			if (!wentToFar && !outdated) {
				component.services(TransportService.class).forEach(t -> t.multicast(msg, this, p));
			}
		}
	}

	@Override
	public String webShortcut() {
		return "irp";
	}

	@Override
	public String getAlgoName() {
		return "IRP";
	}

	public double expirationDate(Message msg) {
		var creationDate = msg.route.first().emissionDate;
		var r = (IRPParms) msg.route.first().routing.parms;
		return creationDate + r.getValidityDuration();
	}

	public double remainingTime(Message msg) {
		return expirationDate(msg) - component.now();
	}

	public boolean isExpired(Message msg) {
		return remainingTime(msg) <= 0;
	}

	@Override
	public List<IRPParms> dataSuggestions() {
		var l = new ArrayList<IRPParms>();

		{
			var p = new IRPParms();
			p.components = Set.of(component.localView().g.pickRandomComponent(new Random()),
					component.localView().g.pickRandomComponent(new Random()));
			p.coverage = 1000;
			p.validityDuration = 1;
			l.add(p);
		}

		{
			var p = new IRPParms();
			p.components = null;
			p.coverage = 3;
			p.validityDuration = 2;
			l.add(p);
		}

		return l;
	}

	@Override
	public ComponentMatcher naturalTarget(IRPParms parms) {
		var p = (IRPParms) parms;
		return ComponentMatcher.multicast(p.components);
	}
}
