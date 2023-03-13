package idawi.transport;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassOperation;
import idawi.knowledge_base.ComponentRef;
import idawi.knowledge_base.MapService;
import idawi.knowledge_base.MiscKnowledgeBase;
import idawi.messaging.Message;
import idawi.routing.Emission;
import idawi.routing.Reception;
import idawi.routing.Route;
import idawi.routing.RoutingParms;
import idawi.routing.RoutingService;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;

public abstract class TransportService extends Service {
	public static Serializer serializer = new JavaSerializer();

	protected final AtomicLong nbMsgReceived = new AtomicLong();

	public long nbOfMsgReceived = 0;

	public TransportService(Component c) {
		super(c);
		registerOperation(new getNbMessagesReceived());
		registerOperation(new neighbors());
	}

	// this is called by transport implementations
	protected final void processIncomingMessage(Message msg) {
		++nbOfMsgReceived;
//		Cout.debug(component + " receives from " + msg.route.components() + ": " + msg.content);
		var reception = new Reception(component, this);
		msg.route.add(reception);

		var kb = component.lookup(MiscKnowledgeBase.class);

		for (var e : msg.route.events()) {
			e.component.component = kb.localComponents.get(e.component);
		}

		component.forEachServiceOfClass(MapService.class, s -> s.map.feedWith(msg.route));

		// if the message was target to this component
		if (msg.destination.componentTarget.test(component.ref())) {
			var s = component.lookup(msg.destination.service());
			s.considerNewMessage(msg);
		}

		// search for the same routing service
		RoutingService rs = component.lookup(reception.previousEmission().routingProtocol());

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
	protected void addEmissionEvent(Message msg, RoutingService r, RoutingParms parms) {
		msg.route.add(new Emission(r, parms, this));
	}

	public abstract String getName();

	public abstract boolean canContact(ComponentRef c);

	public Collection<ComponentRef> believedNeighbors(ComponentRef c) {
		return component.mapService().map.neighbors(c, getClass());
	}

	public Collection<ComponentRef> believedNeighbors() {
		return believedNeighbors(component.ref());
	}

	public abstract Collection<ComponentRef> actualNeighbors();

	protected abstract void multicastImpl(Message msg, Collection<ComponentRef> throughSpecificNeighbors);

	protected abstract void bcastImpl(Message msg);

	public final void multicast(Message msg, Collection<ComponentRef> throughSpecificNeighbors, RoutingService r,
			RoutingParms p) {
		addEmissionEvent(msg, r, p);
		multicastImpl(msg, throughSpecificNeighbors);
		msg.route.removeLast();
	}

	public final void bcast(Message msg, RoutingService r, RoutingParms p) {
		addEmissionEvent(msg, r, p);
		bcastImpl(msg);
		msg.route.removeLast();
	}

	@Override
	public String getFriendlyName() {
		return getName() + " transport";
	}

	public long getNbMessagesReceived() {
		return nbMsgReceived.get();
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
		public Collection<ComponentRef> f() {
			return actualNeighbors();
		}

		@Override
		public String getDescription() {
			return "number of message received so far";
		}
	}

}
