package idawi.transport;

import java.util.ArrayList;
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
import idawi.SupplierEndPoint;
import idawi.messaging.ACK.serviceNotAvailable;
import idawi.messaging.Message;
import idawi.routing.ComponentMatcher.multicast;
import idawi.routing.Entry;
import idawi.routing.RoutingService;
import idawi.service.EncryptionService;

public abstract class TransportService extends Service {
	public long nbMsgReceived = 0;
	public long nbMsgSent;
	public long incomingTraffic;
	public long outGoingTraffic;
	// used to serialize messages for transport
	public final transient IdawiSerializer serializer;
	public final List<TransportListener> listeners = new ArrayList<>();
	// public final AutoForgettingLongList alreadyKnownMsgs = new
	// AutoForgettingLongList(l -> l.size() < 1000);

	public TransportService(Component c) {
		super(c);
		this.serializer = new IdawiSerializer(this);
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

	public abstract String getName();

	protected abstract void multicast(byte[] msgBytes, Collection<Link> outLinks);

	protected void multicast(byte[] msgBytes) {
		multicast(msgBytes, activeOutLinks());
	}

	// this is called by transport implementations
	public synchronized final void processIncomingMessage(Message msg) {
		if (!msg.route.getLast().link.dest.component.equals(component))
			throw new IllegalStateException(
					"route ends by " + msg.route.getLast().link.dest.component + " instead of " + component);

		try {
			Vault vault = msg.content instanceof Vault ? (Vault) msg.content : null;
			++nbMsgReceived;
			incomingTraffic += msg.sizeOf();
			Entry lastRouteEntry = msg.route.getLast();
			// lastRouteEntry.link.dest = this;
			lastRouteEntry.receptionDate = component.now();
			lastRouteEntry.link.latency = lastRouteEntry.duration();
			// Cout.debugSuperVisible(serializer.transportService);
			listeners.forEach(l -> l.msgReceived(this, msg));
			// Cout.debug("-----");

			component.trafficListeners.forEach(l -> l.newMessageReceived(this, msg));

			boolean loopback = msg.route.getFirst().link.src.component.equals(component);
			// if the message was targeted to this component and its the first time it is
			// received
			if (msg.qAddr.targetedComponents.test(component) && (!component.alreadyReceivedMsgs.contains(msg.ID))) {
				var targetService = component.service(msg.qAddr.service, msg.autoStartService);

				if (targetService == null) {
					if (msg.ackReqs != null && msg.ackReqs.contains(serviceNotAvailable.class)) {
						var ack = new serviceNotAvailable(msg.qAddr.service, component, msg);
						send(ack, msg.replyTo, outM -> outM.eot = true);
						System.out.println(
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

					component.need(msg.routingStrategy.routingService).accept(msg);
				}

				component.alreadyReceivedMsgs.add(msg.ID);
			}
		} catch (Throwable err) {
			err(msg, err);
		}
	}

	public final void send(Message msg, Iterable<Link> outLinks, RoutingService r) {

		++nbMsgSent;
		outGoingTraffic += msg.sizeOf();

		// add a link heading to an unknown destination
		msg.route.add(new Entry(new Link(this), r.getClass()));
		listeners.forEach(l -> l.msgSent(this, msg, outLinks));

		if (outLinks == null) {
			if (msg.simulate) {
				// ???
			}

			if (this instanceof Broadcastable tb) {
				tb.bcast(msgToBytes(msg));
			} else {
				multicast(msgToBytes(msg));
			}
		} else {
			for (var l : outLinks)
				if (l.src != this)
					throw new IllegalStateException();

			Set<Link> realSend = new HashSet<Link>();
			Set<Link> fakeEmissions = new HashSet<Link>();
			var sentFromTwin = component.isDigitalTwin();

			for (var outLink : outLinks) {
				// sending from a real component to a digital twin in the only situation
				// the transport is really involved
				var sentToTwin = outLink.dest.component.isDigitalTwin();
				var loop = outLink.dest.component.equals(component);

				if (!sentFromTwin && sentToTwin) {
					realSend.add(outLink);
				} else {
					fakeEmissions.add(outLink);
				}

				outLink.nbMsgs++;
			}

			var msgBytes = msgToBytes(msg);
			multicast(msgBytes, realSend);
			sendToTwin(msg, fakeEmissions);
		}

		component.alreadySentMsgs.add(msg.ID);
	}

	private byte[] msgToBytes(Message msg) {
		var es = component.service(EncryptionService.class);

		// if encryption is required
		if (es != null) {
			msg.content = new Vault(msg.content, es.rsa, serializer);
			throw new IllegalStateException();
		}

		return serializer.toBytes(msg);
	}

	private void sendToTwin(Message msg, Collection<Link> links) {
		sendToTwin(serializer.toBytes(links), links);
	}

	protected void sendToTwin(byte[] msg, Collection<Link> links) {
		for (var l : links) {
			sendToTwin(msg, l);
		}
	}

	protected void sendToTwin(byte[] msg, Link l) {
		Idawi.agenda.schedule(new Event<PointInTime>("message reception", new PointInTime(now() + l.latency())) {
			@Override
			public void run() {
				// Cout.debug("bast");

				// Cout.debugSuperVisible(":) " + l + " "+ l.dest);
				try {
					l.dest.processIncomingMessage((Message) l.dest.serializer.fromBytes(msg));
				} catch (Throwable e) {
					e.printStackTrace();
					System.exit(0);
				}
			}
		});
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

	public class getNbMessagesReceived extends SupplierEndPoint<Long> {
		@Override
		public Long get() {
			return getNbMessagesReceived();
		}

		@Override
		public String getDescription() {
			return "number of message received so far";
		}
	}

	public class neighbors extends SupplierEndPoint<List<Link>> {
		@Override
		public List<Link> get() {
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
