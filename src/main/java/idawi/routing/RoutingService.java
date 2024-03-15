package idawi.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import idawi.Component;
import idawi.EndpointParameterList;
import idawi.InnerClassEndpoint;
import idawi.RemotelyRunningEndpoint;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import idawi.messaging.RoutingStrategy;
import idawi.transport.TransportService;
import toools.io.Cout;
import toools.util.Date;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public abstract class RoutingService<D extends RoutingData> extends Service implements BiConsumer<Message, D> {
	protected final AtomicLong nbMsgReceived = new AtomicLong();
	public final List<RoutingListener> listeners = new ArrayList<>();

	
	public long nbMessagesInitiated;

	public RoutingService(Component node) {
		super(node);
	}

	public void accept(Message msg) {
		accept(msg, defaultData());
	}
	


	@Override
	public String getFriendlyName() {
		return getAlgoName() + " routing protocol";
	}

	protected List<TransportService> transports() {
		return component.services(TransportService.class);
	}

	protected D convert(RoutingData p) {
		return (D) p;
	}

	public abstract ComponentMatcher naturalTarget(D parms);

	public abstract String getAlgoName();

	public abstract List<D> dataSuggestions();

	public D defaultData() {
		return dataSuggestions().get(0);
	}

	public void send(Object value, boolean eot, ComponentMatcher r, Class<? extends Service> s, String queueID) {
//		Cout.debugSuperVisible("send " + value);
		++nbMessagesInitiated;
		var dest = new MessageQDestination();
		dest.queueID = queueID;
		dest.service = s;
		dest.componentMatcher = r;

		send(value, eot, dest, defaultData());
	}

	public void send(Object content, boolean eot, Destination dest, D parms) {
		Objects.requireNonNull(dest.service());
		var preferredRoutingStrategy = new RoutingStrategy(getClass(), parms);
		var msg = new Message(dest, preferredRoutingStrategy, content);
		msg.eot = eot;
		accept(msg, parms);
	}

	public void send(Object value, boolean eot, Destination dest) {
		send(value, eot, dest, defaultData());
	}

	public void send(String value, boolean eot, RemotelyRunningEndpoint o) {
		send(value, eot, o.getOperationInputQueueDestination());
	}

	public RemotelyRunningEndpoint exec(Class<? extends Service> service, Class<? extends InnerClassEndpoint> o,
			D parms, ComponentMatcher re, boolean returnQ, Object initialInputData, boolean eot) {
		return exec(service, o, parms, re, returnQ ? createUniqueQueue("returnQ") : null, initialInputData, eot);
	}

	public RemotelyRunningEndpoint exec(Component to, Class<? extends Service> service,
			Class<? extends InnerClassEndpoint> o, Object initialInputData, boolean eot) {
		return exec(service, o, null, ComponentMatcher.unicast(to), true, initialInputData, eot);
	}

	public RemotelyRunningEndpoint exec(Component to, Class<? extends InnerClassEndpoint> o, Object initialInputData, boolean eot) {
		return exec((Class<? extends Service>) o.getEnclosingClass(), o, null, ComponentMatcher.unicast(to), true,
				initialInputData, eot);
	}

	public RemotelyRunningEndpoint exec(Component to, Class<? extends Service> service,
			Class<? extends InnerClassEndpoint> o, D parms, Object initialInputData, boolean eot) {
		return exec(service, o, parms, ComponentMatcher.unicast(to), true, initialInputData, eot);
	}

	public RemotelyRunningEndpoint exec(Class<? extends Service> service, Class<? extends InnerClassEndpoint> o,
			Object initialInputData, boolean eot) {
		D parms = defaultData();
		return exec(service, o, parms, naturalTarget(parms), true, initialInputData, eot);
	}

	public RemotelyRunningEndpoint exec(Class<? extends Service> service, Class<? extends InnerClassEndpoint> o,
			D parms, ComponentMatcher matcher, MessageQueue returnQ, Object initialInputData, boolean eot) {

		// Cout.debugSuperVisible("exec " + o.getSimpleName() + " " + initialInputData);
		var dest = new MessageODestination(service, o);
		dest.service = service;
		dest.invocationDate = Date.timeNs();
		dest.componentMatcher = matcher;
		var r = new RemotelyRunningEndpoint();

		if (returnQ != null) {
			r.returnQ = returnQ;
			dest.replyTo = new MessageQDestination();
			dest.replyTo.componentMatcher = ComponentMatcher.unicast(component);
			dest.replyTo.queueID = r.returnQ.name;
			dest.replyTo.service = r.returnQ.service.getClass();
		}

		send(initialInputData, eot, dest, parms);
		r.destination = dest;
		return r;
	}

	public Object exec_rpc(Component to, Class<? extends InnerClassEndpoint> o, Object parms) {
		return exec_rpc(to, (Class<? extends Service>) o.getEnclosingClass(), o, parms);
	}

	public static double RPC_TIMEOUT = 5;

	public Object exec_rpc(Component to, Class<? extends Service> service, Class<? extends InnerClassEndpoint> o,
			Object parms) {
		var r = exec(service, o, defaultData(), ComponentMatcher.unicast(to), createUniqueQueue("returnQ"),
				new EndpointParameterList(parms), true).returnQ.poll_sync(RPC_TIMEOUT);

		if (r.content instanceof Throwable) {
			if (r.content instanceof RuntimeException) {
				throw (RuntimeException) r.content;
			} else {
				throw new RuntimeException((Throwable) r.content);
			}
		} else {
			return r.content;
		}
	}
	
	public class dummyService extends InnerClassEndpoint {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			Cout.debugSuperVisible(component + " received: " + in.poll_sync());
		}

		@Override
		public String getDescription() {
			return "do nothing";
		}
	}

	public class ping extends InnerClassEndpoint {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			Cout.debugSuperVisible("PING");
			var m = in.poll_sync();
			// sends back the ping message to the caller
			reply(m, m, true);
		}

		@Override
		public String getDescription() {
			return "sends back the exec message";
		}
	}

	public class test1 extends InnerClassEndpoint {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			Cout.debugSuperVisible(this);
		}

		@Override
		public String getDescription() {
			return "receives but does nothing";
		}
	}

	public class test2 extends TypedInnerClassEndpoint {

		public void impl() {
			Cout.debugSuperVisible(this);
		}

		@Override
		public String getDescription() {
			return "just send EOT as the sole response";
		}
	}

	public class suggestParms extends TypedInnerClassEndpoint {

		@Override
		public String getDescription() {
			return "suggestParms";
		}

		public List<String> impl() {
			return dataSuggestions().stream().map(d -> d.toURLElement()).toList();
		}
	}

	public MessageQueue ping(Set<Component> targets, D parms) {
		return exec(getClass(), ping.class, parms, ComponentMatcher.multicast(targets), true, "ping test", true).returnQ;
	}

	public MessageQueue ping(Set<Component> targets) {
		return ping(targets, defaultData());
	}

	public MessageQueue ping(Component target) {
		return ping(Set.of(target), defaultData());
	}

	public MessageQueue ping(D parms) {
		return exec(getClass(), ping.class, parms, naturalTarget(parms), true, "ping", true).returnQ;
	}

	public MessageQueue ping() {
		return ping(defaultData());
	}

	public static final Stream<RoutingService> routings(Stream<Component> cs) {
		return cs.flatMap(c -> c.services(RoutingService.class).stream());
	}

	public static final Stream<TransportService> transports(Stream<Component> cs) {
		return cs.flatMap(c -> c.services(TransportService.class).stream());
	}

	public static final long nbReceptions(Stream<Component> cs) {
		return transports(cs).map(r -> r.nbMsgReceived).reduce(0L, Long::sum).longValue();
	}

}
