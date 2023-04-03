package idawi.routing.irp;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import idawi.Component;
import idawi.knowledge_base.DigitalTwinListener;
import idawi.messaging.Message;
import idawi.routing.RoutingService;
import idawi.routing.TargetComponents;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class IRP extends RoutingService<NEParms> {
	public final LongSet alreadyReceivedMsgs = new LongOpenHashSet();
	public final ConcurrentHashMap<Long, Message> aliveMessages = new ConcurrentHashMap<>();

	public IRP(Component node) {
		super(node);

		component.digitalTwinService().listeners.add(new DigitalTwinListener() {

			@Override
			public void newComponent(Component p) {
			}

			@Override
			public void componentHasGone(Component a) {
			}

			@Override
			public void newInteraction(TransportService from, TransportService to) {
				if (component.equals(from.component)) {
					var tt = component.lookup(from.getClass());

					synchronized (aliveMessages) {
						for (Message msg : aliveMessages.values()) {
							tt.multicast(msg, Set.of(to.find(to.component)), IRP.this, msg.route.last().routingParms());
						}
					}
				}
			}

			@Override
			public void interactionStopped(TransportService from, TransportService to) {
			}
		});
	}

	@Override
	public void accept(Message msg, NEParms p) {
		// the message was never received
		if (!alreadyReceivedMsgs.contains(msg.ID)) {
			alreadyReceivedMsgs.add(msg.ID);

			var parms = (NEParms) msg.route.lastEmission().routingParms();

			boolean wentToFar = parms.coverage > msg.route.nbEvents() / 2;
			boolean outdated = parms.validityDuration > msg.route.duration();

			if (!wentToFar && !outdated) {
				component.services(TransportService.class).forEach(t -> t.bcast(msg, this, p));
			}
		}
	}

	@Override
	public String getAlgoName() {
		return "NE";
	}

	public double expirationDate(Message msg) {
		var creationDate = msg.route.initialEmission().date();
		var r = (NEParms) msg.route.initialEmission().routingParms();
		return creationDate + r.getValidityDuration();
	}

	public double remainingTime(Message msg) {
		return expirationDate(msg) - component.now();
	}

	public boolean isExpired(Message msg) {
		return remainingTime(msg) <= 0;
	}

	@Override
	public NEParms createDefaultRoutingParms() {
		return new NEParms();
	}

	@Override
	public TargetComponents naturalTarget(NEParms parms) {
		var p = (NEParms) parms;
		return new TargetComponents.Multicast(p.componentNames);
	}
}
