package idawi.transport;

import java.util.Collection;
import java.util.List;

import idawi.Component;
import idawi.Event;
import idawi.PointInTime;
import idawi.RuntimeEngine;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.Message;
import idawi.routing.RoutingData;
import idawi.routing.RoutingService;
import toools.io.Cout;

public abstract class TransportService extends Service {
	public long nbMsgReceived = 0;
	public long nbMsgSent;
	public long incomingTraffic;
	public long outGoingTraffic;

	public TransportService(Component c) {
		super(c);
		c.localView().g.markLinkActive(this, this); // loopback
	}

	@Override
	public String toString() {
		return component + "/" + getName();
	}

	// this is called by transport implementations
	protected final void processIncomingMessage(Message msg) {
		Cout.debug(" " + component + " receives " + msg);
		++nbMsgReceived;
		incomingTraffic += msg.sizeOf();
		var last = msg.route.last();
		last.receptionDate = component.now();
		last.link.latency = last.duration();

		component.trafficListeners.forEach(l -> l.newMessageReceived(this, msg));

		// if the message was targeted to this component and its the first time it is
		// received
		if (msg.destination.componentMatcher.test(component)
				&& msg.route.recipients().filter(t -> t.component.equals(component)).count() == 1) {
			var s = component.service(msg.destination.service(), msg.destination.autoStartService);

			if (s == null) {
				System.err.println(
						"component " + component + " does not have service " + msg.destination.service().getName());
			} else {
//				System.err.println(component + " DELIVER " + msg.route);
				s.considerNewMessage(msg);
			}
		} else {
			// System.err.println(component + " DROP " + msg.route);
		}

		forward(msg);
	}

	private void forward(Message msg) {
		// search for the routing service it was initially sent
		var rs = component.service(msg.preferredRoutingStrategy.routingService, true);
		RoutingData routingParms;

		if (rs == null) {
			rs = component.defaultRoutingProtocol();
			routingParms = rs.defaultData();
		} else {
			routingParms = msg.preferredRoutingStrategy.parms;
		}

		// no such routing protocol running here
		// dunno what to do with the message
		// so just pass it around
		component.defaultRoutingProtocol().accept(msg, routingParms);
	}

	public abstract String getName();

	public abstract boolean canContact(Component c);

	protected abstract void sendImpl(Message msg);

	public final void send(Message msg, Collection<Link> outLinks, RoutingService r, RoutingData parms) {

		 Cout.debug(" " + component + " sends: " + msg);

		 for (var outLink : outLinks) {
			msg.route.add(outLink, r);
			++nbMsgSent;
			outGoingTraffic += msg.sizeOf();

			// sending from a real component to a digital twin in the only situation
			// the network is involved
			var sentFromTwin = component.isDigitalTwin();
			var loop = outLink.dest.component.equals(component);
			var sentToTwin = outLink.dest.component.isDigitalTwin();
			var simulatedNodeTarget = searchSimlatedComponent(outLink.dest.component);

			if (sentFromTwin) {
				fakeSend(msg, outLink, outLink.dest);
			} else if (simulatedNodeTarget != null) {
				var to = simulatedNodeTarget.service(outLink.dest.getClass(), true);
				fakeSend(msg, outLink, to);
			} else if (loop) {
				fakeSend(msg, outLink, outLink.dest);
			} else {
				sendImpl(msg);
			}

			msg.route.removeLast();
		}
	}

	private Component searchSimlatedComponent(Component search) {
		for (var c : component.simulatedComponents) {
			if (c.equals(search)) {
				return c;
			}
		}

		return null;
	}

	private void fakeSend(Message msg, Link outLink, TransportService to) {
		var msgClone = msg.clone(component.serializer);
		double actualLatency = outLink.latency;

		RuntimeEngine.offer(
				new Event<PointInTime>("message reception " + msgClone.ID, new PointInTime(now() + actualLatency)) {
					@Override
					public void run() {
						System.out.println("fae sed");
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
		return getName() + " transport";
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

}
