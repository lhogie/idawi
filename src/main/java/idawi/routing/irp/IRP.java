package idawi.routing.irp;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import idawi.Component;
import idawi.knowledge_base.ComponentRef;
import idawi.knowledge_base.NetworkTopologyListener;
import idawi.knowledge_base.info.DirectedLink;
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

		component.mapService().map.listeners.add(new NetworkTopologyListener() {

			@Override
			public void newInteraction(DirectedLink l) {
				if (component.ref().equals(l.src)) {
					var tt = component.lookup(l.transport);

					synchronized (aliveMessages) {
						for (Message msg : aliveMessages.values()) {
							tt.multicast(msg, Set.of(l.dest), IRP.this, msg.route.last().routingParms());
						}
					}
				}
			}

			@Override
			public void newComponent(ComponentRef p) {
			}

			@Override
			public void interactionStopped(DirectedLink l) {
			}

			@Override
			public void componentHasGone(ComponentRef a) {
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
	public NEParms decode(String s) {
		var to = new NEParms();

		for (var n : s.split(" *, *")) {
			to.componentNames.add(component.mapService().map.lookup(n));
		}

		return to;
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
