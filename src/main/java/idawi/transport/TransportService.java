package idawi.transport;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.stream.Stream;

import idawi.Component;
import idawi.Event;
import idawi.RuntimeEngine;
import idawi.Service;
import idawi.SpecificTime;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.Message;
import idawi.routing.RouteListener;
import idawi.routing.RoutingData;
import idawi.routing.RoutingService;
import toools.io.Cout;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;

public abstract class TransportService extends Service implements Externalizable {
	// used to serialized messages for transport
	public static Serializer serializer = new JavaSerializer();
	public long nbOfMsgReceived = 0;

	public TransportService() {
	}

	public TransportService(Component c) {
		super(c);
	}

	@Override
	public String toString() {
		return component + "/" + getName();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(component);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		setComponent((Component) in.readObject());
	}

	// this is called by transport implementations
	protected final void processIncomingMessage(Message msg) {
		Cout.debug(" " + component + " receives " + msg);
		++nbOfMsgReceived;

		if (component.isDigitalTwin())
			throw new IllegalStateException();

		msg.route.entries().forEach(e -> e.link = component.localView().ensureTwinLinkExists(e.link));
		msg.route.last().receptionDate = component.now();

		component.forEachServiceOfClass(RouteListener.class, s -> s.feedWith(msg.route));

		// if the message was targeted to this component and its the first time it is
		// received
		if (msg.destination.componentMatcher.test(component)
				&& msg.route.recipients().filter(t -> t.component.equals(component)).count() == 1) {
			var s = component.need(msg.destination.service());

			if (s == null) {
				System.err.println(
						"component " + component + " does not have service " + msg.destination.service().getName());
			} else {
				System.err.println(component + " DELIVER " + msg.route);
				s.considerNewMessage(msg);
			}
		} else {
			System.err.println(component + " DROP " + msg.route);
		}

		forward(msg);
	}

	private void forward(Message msg) {
		// search for the same routing service
		var rs = component.lookup(msg.route.last().routingProtocol());

		// no such routing protocol running here
		if (rs == null) {
			// dunno what to do with the message
			// so just pass it around
			component.defaultRoutingProtocol().accept(msg, msg.currentRoutingParameters());
		} else {
			rs.accept(msg, msg.currentRoutingParameters());
		}
	}

	public abstract String getName();

	public abstract boolean canContact(Component c);

	protected abstract void sendImpl(Message msg);

	public final void send(Message msg, Collection<Link> outLinks, RoutingService r, RoutingData parms) {

		// Cout.debug(" " + component + " sends: " + msg);
		for (var outLink : outLinks) {
			msg.route.add(outLink, r);

			// sending from a real component to a digital twin in the only situation
			// the network is involved
			if (!component.isDigitalTwin() && outLink.dest.component.isDigitalTwin()) {
				sendImpl(msg);
			} else {
				var c = msg.clone();

				RuntimeEngine.offer(new Event<SpecificTime>(new SpecificTime(now() + outLink.latency)) {
					@Override
					public void run() {
						try {
							outLink.dest.processIncomingMessage(c);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				});
			}

			msg.route.removeLast();
		}
	}

	public final void bcast(Message msg, RoutingService r, RoutingData parms) {
		send(msg, outLinks().toList(), r, parms);
	}

	public Stream<Link> outLinks() {
		return component.localView().links().stream().filter(l -> l.activity.availableAt(now()) && l.src.equals(this));
	}

	@Override
	public String getFriendlyName() {
		return getName() + " transport";
	}

	public long getNbMessagesReceived() {
		return nbOfMsgReceived;
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
		public OutLinks f() {
			return new OutLinks(outLinks());
		}

		@Override
		public String getDescription() {
			return "get the neighborhood";
		}
	}

	public abstract void dispose(Link l);

	public Link outTo(Component dest) {
		return outTo(dest.need(getClass()));
	}

	public Link outTo(TransportService to) {
		var l = component.localView().findLink(this, to);

		if (l == null) {
			component.localView().links().add(l = new Link(this, to));
		} else {
			l.activity.add(new TimeFrame(now()));
		}

		return l;
	}

	public void inoutTo(Component c) {
		outTo(c);
		c.need(getClass()).outTo(this);
	}

	public void connectTo(TransportService a, boolean bidi) {
		outTo(a);

		if (bidi) {
			a.outTo(this);
		}
	}
}
