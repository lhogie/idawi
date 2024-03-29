package idawi.transport;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToLongFunction;

import idawi.Component;
import idawi.Event;
import idawi.Idawi;
import idawi.IdawiSerializer;
import idawi.PointInTime;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.Message;
import idawi.routing.ComponentMatcher.multicast;
import idawi.routing.Entry;
import idawi.routing.RoutingParameters;
import idawi.routing.RoutingService;
import idawi.service.EncryptionService;
import toools.io.Cout;

public abstract class TransportService extends Service {
	public long nbMsgReceived = 0;
	public long nbMsgSent;
	public long incomingTraffic;
	public long outGoingTraffic;
	// used to serialize messages for transport
	public final transient IdawiSerializer serializer;

//	public final AutoForgettingLongList alreadyKnownMsgs = new AutoForgettingLongList(l -> l.size() < 1000);

	public TransportService(Component c) {
		super(c);
		serializer = new IdawiSerializer(this);
//		 c.localView().g.markLinkActive(this, this); // loopback
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
		try {
			Vault vault = msg.content instanceof Vault ? (Vault) msg.content : null;
			Cout.debug(this + " receives " + msg);
			++nbMsgReceived;
			incomingTraffic += msg.sizeOf();
			Entry last = msg.route.getLast();
//			last.link.dest = this;
			last.receptionDate = component.now();
			last.link.latency = last.duration();

			component.trafficListeners.forEach(l -> l.newMessageReceived(this, msg));

			boolean loopback = msg.route.getFirst().link.src.component.equals(component);
			// if the message was targeted to this component and its the first time it is
			// received
			if (msg.qAddr.targetedComponents.test(component) && (!component.alreadyReceivedMsgs.contains(msg.ID))) {
				var targetService = component.service(msg.qAddr.service, msg.autoStartService);

				if (targetService == null) {
					if (msg.alertServiceNotAvailable) {
						throw new IllegalStateException(
								"component " + component + " does not have service " + msg.qAddr.service.getName());
					}
				} else {
					targetService.process(msg);
				}
			}

			synchronized (component) {
				boolean msgTargettedToMeOnly = msg.qAddr.targetedComponents instanceof multicast to
						&& to.target.size() == 1 && to.target.contains(component);

				if (!msgTargettedToMeOnly) {
					if (vault != null) {
						msg.content = vault;
					}

					considerForForwarding(msg);
				}

				component.alreadyReceivedMsgs.add(msg.ID);
			}
		} catch (Throwable err) {
			err(msg, err);
		}
	}

	private void considerForForwarding(Message msg) {
		// search for the routing service it was initially sent
		var rs = component.service(msg.initialRoutingStrategy.routingService, true);
		RoutingParameters routingParms;

		if (rs == null) {
			rs = component.defaultRoutingProtocol();
			routingParms = rs.defaultParameters();
		} else {
			routingParms = msg.initialRoutingStrategy.parms;
		}

		rs.accept(msg, routingParms);
	}

	public abstract String getName();

	protected abstract void multicast(byte[] msgBytes, Collection<Link> outLinks);

	protected abstract void bcast(byte[] msgBytes);

	public final void send(Message msg, Collection<Link> outLinks, RoutingService r, RoutingParameters parms) {
		var sentFromTwin = component.isDigitalTwin();
		++nbMsgSent;
		outGoingTraffic += msg.sizeOf();

		// add a link heading to an unknown destination
		msg.route.add(new Entry(new Link(this), r));
		Cout.debug(component + " uses '" + getName() + "' to send: " + msg);

		if (outLinks == null) {
			bcast(msgToBytes(msg));
		} else {
			Set<Link> realSend = new HashSet<Link>();
			Set<Link> fakeEmissions = new HashSet<Link>();

			for (var outLink : outLinks) {
				// sending from a real component to a digital twin in the only situation
				// the transport is really involved
				var loop = outLink.dest.component.equals(component);

				if (sentFromTwin || msg.simulate || loop) {
					fakeEmissions.add(outLink);
				} else {
					realSend.add(outLink);
				}

				outLink.nbMsgs++;
			}

			var msgBytes = msgToBytes(msg);
			multicast(msgBytes, realSend);
			fakeSend(msg, fakeEmissions);
		}

		component.alreadySentMsgs.add(msg.ID);
	}

	private byte[] msgToBytes(Message msg) {
		var es = component.service(EncryptionService.class);

		// if need to encrypt
		if (es != null) {
			msg.content = new Vault(msg.content, es.rsa, serializer);
			throw new IllegalStateException();
		}

		return serializer.toBytes(msg);
	}

	private void fakeSend(Message msg, Set<Link> fakeEmissions) {
		for (var l : fakeEmissions) {
			var msgClone = msg.clone(serializer);
			double actualLatency = l.latency();

			Idawi.agenda.schedule(
					new Event<PointInTime>("message reception " + msgClone.ID, new PointInTime(now() + actualLatency)) {
						@Override
						public void run() {
//							Cout.debugSuperVisible(":)");
							try {
								l.dest.processIncomingMessage(msgClone);
							} catch (Throwable e) {
								e.printStackTrace();
							}
						}
					});
		}
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
