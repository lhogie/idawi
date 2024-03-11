package idawi.transport;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.ToLongFunction;

import idawi.Component;
import idawi.Event;
import idawi.Idawi;
import idawi.PointInTime;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.Message;
import idawi.routing.AutoForgettingLongList;
import idawi.routing.ComponentMatcher;
import idawi.routing.RoutingData;
import idawi.routing.RoutingService;
import toools.io.Cout;

public abstract class TransportService extends Service {
	public long nbMsgReceived = 0;
	public long nbMsgSent;
	public long incomingTraffic;
	public long outGoingTraffic;
//	public final AutoForgettingLongList alreadyKnownMsgs = new AutoForgettingLongList(l -> l.size() < 1000);

	public TransportService(Component c) {
		super(c);
		// c.localView().g.markLinkActive(this, this); // loopback
	}

	@Override
	public long sizeOf() {
		return 8 * 4 + super.sizeOf();
	}

	@Override
	public String toString() {
		return component + "/" + getName();
	}

	// this is called by transport implementations
	protected synchronized final void processIncomingMessage(Message msg) {
		Cout.debug(this + " receives " + msg);
		++nbMsgReceived;
		incomingTraffic += msg.sizeOf();
		var last = msg.route.last();
		last.receptionDate = component.now();
		last.link.latency = last.duration();

		component.trafficListeners.forEach(l -> l.newMessageReceived(this, msg));

		// if the message was targeted to this component and its the first time it is
		// received
		if (msg.destination.componentMatcher.test(component) && !component.alreadyKnownMsgs.contains(msg.ID)) {
			var targetService = component.service(msg.destination.service(), msg.destination.autoStartService);

			if (targetService == null) {
				if (msg.alertServiceNotAvailable) {
					reply(msg, "no such service", true);
				} else {
					System.err.println(
							"component " + component + " does not have service " + msg.destination.service().getName());
				}
			} else {
				targetService.process(msg);
			}
		}

		synchronized (component) {
			if (!msgTargettedToMeOnly(msg.destination.componentMatcher)) {
				considerForForwarding(msg);
			}

			component.alreadyKnownMsgs.add(msg.ID);
		}
	}

	private boolean msgTargettedToMeOnly(ComponentMatcher m) {
		if (m instanceof ComponentMatcher.multicast) {
			ComponentMatcher.multicast to = (ComponentMatcher.multicast) m;
			return to.target.size() == 1 && to.target.contains(component);
		} else {
			return false;
		}
	}

	private void considerForForwarding(Message msg) {
		// search for the routing service it was initially sent
		var rs = component.service(msg.routingStrategy.routingService, true);
		RoutingData routingParms;

		if (rs == null) {
			rs = component.defaultRoutingProtocol();
			routingParms = rs.defaultData();
		} else {
			routingParms = msg.routingStrategy.parms;
		}

		rs.accept(msg, routingParms);
	}

	public abstract String getName();


	protected abstract void sendImpl(Message msg);

	public final void send(Message msg, Collection<Link> outLinks, RoutingService r, RoutingData parms) {
		Cout.debug(" " + component + " uses '" + getName() + "' to send: " + msg);
		component.alreadyKnownMsgs.add(msg.ID);

		var impactedLinks = new HashSet<>(outLinks.stream().flatMap(l -> l.impactedLinks().stream()).toList());

		for (var outLink : impactedLinks) {
			msg.route.add(outLink, r);
			++nbMsgSent;
			outGoingTraffic += msg.sizeOf();

			// sending from a real component to a digital twin in the only situation
			// the transport is really involved
			var sentFromTwin = component.isDigitalTwin();
			var sentToTwin = outLink.dest.component.isDigitalTwin();
//			System.out.println(outLink.dest.component + "  and " + component);
			var loop = outLink.dest.component.equals(component);

			if (sentFromTwin) {
				fakeSend(msg, outLink, outLink.dest);
			} else if (msg.simulate) {
				fakeSend(msg, outLink, outLink.dest);
			} else if (loop) {
				fakeSend(msg, outLink, outLink.dest);
			} else {
				sendImpl(msg);
			}

			outLink.nbMsgs++;
			outLink.traffic += msg.sizeOf();
			msg.route.removeLast();
		}
	}


	private void fakeSend(Message msg, Link outLink, TransportService to) {
		var msgClone = msg.clone(component.secureSerializer);
		double actualLatency = outLink.latency();

		Idawi.agenda.schedule(
				new Event<PointInTime>("message reception " + msgClone.ID, new PointInTime(now() + actualLatency)) {
					@Override
					public void run() {
//						Cout.debugSuperVisible(":)");
						try {
							to.processIncomingMessage(msgClone);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				});
	}

	public final void multicast(Message msg, RoutingService r, RoutingData parms) {
		send(msg, activeOutLinks(), r, parms);
	}

	public List<Link> activeOutLinks() {
		return component.localView().g.findLinks(l -> l.isActive() && l.src.equals(this));
	}

	@Override
	public String getFriendlyName() {
		return getName();
	}

	public long getNbMessagesReceived() {
		return nbMsgReceived;
	}

	public class getNbMessagesReceived extends TypedInnerClassEndpoint {
		public long f() {
			return getNbMessagesReceived();
		}

		@Override
		public String getDescription() {
			return "number of message received so far";
		}
	}

	public class neighbors extends TypedInnerClassEndpoint {
		public List<Link> f() {
			return activeOutLinks();
		}

		@Override
		public String getDescription() {
			return "get the neighborhood";
		}
	}

	public abstract void dispose(Link l);

	public static <T extends TransportService> long sum(Collection<Component> components, Class<T> transport,
			ToLongFunction<T> f) {
		return components.stream().flatMap(c -> c.services(transport).stream()).mapToLong(f).reduce(0, Long::sum);
	}

	public abstract double latency();

}
