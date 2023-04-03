package idawi.transport;

import java.util.Collection;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassOperation;
import idawi.knowledge_base.DigitalTwinService;
import idawi.messaging.Message;
import idawi.routing.Emission;
import idawi.routing.Reception;
import idawi.routing.RoutingData;
import idawi.routing.RoutingService;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;

public abstract class TransportService extends Service {
	public static Serializer serializer = new JavaSerializer();

	public long nbOfMsgReceived = 0;

	private Neighborhood neighborhood = new Neighborhood();

	public TransportService(Component c) {
		super(c);
		registerOperation(new getNbMessagesReceived());
		registerOperation(new neighbors());
	}

	public OutNeighbor find(Component c) {
		for (var neighbor : neighborhood) {
			if (neighbor.transport.component.equals(c)) {
				return neighbor;
			}
		}
		
		return null;
	}

	
	// this is called by transport implementations
	protected final void processIncomingMessage(Message msg) {
		++nbOfMsgReceived;
//		Cout.debug(component + " receives from " + msg.route.components() + ": " + msg.content);
		var reception = new Reception(this);
		msg.route.add(reception);

		var kb = component.lookup(DigitalTwinService.class);

		for (var e : msg.route.events()) {
			e.transport = kb.f(e.transport);
		}

		component.forEachServiceOfClass(DigitalTwinService.class, s -> s.feedWith(msg.route));

		// if the message was target to this component
		if (msg.destination.componentTarget.test(component)) {
			var s = component.lookup(msg.destination.service());
			s.considerNewMessage(msg);
		}

		// search for the same routing service
		var rs = component.lookup(reception.previousEmission().routingProtocol());

		// no such routing protocol running here
		if (rs == null) {
			// dunno what to do with the message
			// so just pass it around
			component.bb().accept(msg, msg.currentRoutingParameters());
		} else {
			rs.accept(msg, msg.currentRoutingParameters());
		}
	}

	// called just before emission by transport implementations
	protected void addEmissionEvent(Message msg, RoutingService r, RoutingData parms) {
		msg.route.add(new Emission(r, parms, this));
	}

	public abstract String getName();

	public abstract boolean canContact(Component c);

	public Neighborhood neighborhood() {
		return neighborhood;
	}

	protected abstract void multicastImpl(Message msg, Collection<OutNeighbor> throughSpecificNeighbors);

	protected abstract void bcastImpl(Message msg);

	public final void multicast(Message msg, Collection<OutNeighbor> throughSpecificNeighbors, RoutingService r,
			RoutingData p) {
		addEmissionEvent(msg, r, p);
		multicastImpl(msg, throughSpecificNeighbors);
		msg.route.removeLast();
	}

	public final void bcast(Message msg, RoutingService r, RoutingData p) {
		addEmissionEvent(msg, r, p);
		bcastImpl(msg);
		msg.route.removeLast();
	}

	@Override
	public String getFriendlyName() {
		return getName() + " transport";
	}

	public long getNbMessagesReceived() {
		return nbOfMsgReceived;
	}

	public class getNbMessagesReceived extends TypedInnerClassOperation {
		public long f() {
			return getNbMessagesReceived();
		}

		@Override
		public String getDescription() {
			return "number of message received so far";
		}
	}

	public class neighbors extends TypedInnerClassOperation {
		public Neighborhood f() {
			return neighborhood();
		}

		@Override
		public String getDescription() {
			return "get the neighborhood";
		}
	}

	public abstract Collection<Component> actualNeighbors();

}
