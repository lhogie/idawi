package idawi.routing;

import java.util.ArrayList;
import java.util.List;
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

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public abstract class RoutingService<P extends RoutingParameters> extends Service implements BiConsumer<Message, P> {
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
	public String getDescription() {
		return getAlgoName() + " routing protocol";
	}

	protected List<TransportService> transports() {
		return component.services(TransportService.class);
	}

	protected P convert(RoutingParameters p) {
		return (P) p;
	}

	public abstract ComponentMatcher defaultMatcher(P parms);

	public abstract String getAlgoName();

	public abstract List<P> dataSuggestions();

	public P defaultData() {
		return dataSuggestions().getFirst();
	}

	@Override
	public final void accept(Message msg, P parms) {
		++nbMessagesInitiated;

		if (msg.initialRoutingStrategy == null) {
			msg.initialRoutingStrategy = new RoutingStrategy(this, parms);
		}

		acceptImpl(msg, parms);
	}

	protected abstract void acceptImpl(Message msg, P parms);

	public RemotelyRunningEndpoint exec(ComponentMatcher re, Class<? extends Service> service,
			Class<? extends InnerClassEndpoint> o, P parms, Object initialInputData, boolean eot, String queueName) {
		var r = new RemotelyRunningEndpoint();
		r.returnQ = createUniqueQueue("return-");

		var msg = new Message();
		msg.endpointID = o;
		msg.replyTo = new QueueAddress();
		msg.replyTo.targetedComponents = ComponentMatcher.unicast(component);
		msg.replyTo.service = getClass();
		msg.replyTo.queueID = r.returnQ.name;
		msg.content = initialInputData;

		msg.qAddr = new QueueAddress();
		msg.qAddr.targetedComponents = re;
		msg.qAddr.service = service;
		msg.qAddr.queueID = queueName == null ? o.getSimpleName() + "@" + now() : queueName;

		r.destination = msg.qAddr;
		accept(msg, parms);
		return r;
	}

	public void send(Object content, QueueAddress dest) {
		send(content, true, dest);
	}

	public void send(Object content, boolean eot, QueueAddress dest) {
		exec(dest.targetedComponents, dest.service, Service.deliverToQueue.class, defaultData(), content, eot,
				dest.queueID);
	}

	public RemotelyRunningEndpoint exec(ComponentMatcher re, Class<? extends Service> service,
			Class<? extends InnerClassEndpoint> o, P parms, Object initialInputData, boolean eot) {
		return exec(re, service, o, parms, initialInputData, eot, null);
	}

	public RemotelyRunningEndpoint exec(Component to, Class<? extends Service> service,
			Class<? extends InnerClassEndpoint> o, Object initialInputData, boolean eot) {
		return exec(ComponentMatcher.unicast(to), service, o, null, initialInputData, eot);
	}

	public RemotelyRunningEndpoint exec(Component to, Class<? extends InnerClassEndpoint> o, Object initialInputData,
			boolean eot) {
		return exec(ComponentMatcher.unicast(to), (Class<? extends Service>) o.getEnclosingClass(), o, null,
				initialInputData, eot);
	}

	public RemotelyRunningEndpoint exec(Component to, Class<? extends Service> service,
			Class<? extends InnerClassEndpoint> o, P parms, Object initialInputData, boolean eot) {
		return exec(ComponentMatcher.unicast(to), service, o, parms, initialInputData, eot);
	}

	public RemotelyRunningEndpoint exec(Class<? extends Service> service, Class<? extends InnerClassEndpoint> o,
			Object initialInputData, boolean eot) {
		P parms = defaultData();
		return exec(defaultMatcher(parms), service, o, parms, initialInputData, eot);
	}

	public static double RPC_TIMEOUT = 5;

	public Object exec_rpc(Component to, Class<? extends Service> service, Class<? extends InnerClassEndpoint> o,
			Object parms) {
		var rq = exec(to, service, o, new EndpointParameterList(parms), true).returnQ;
		rq.poll_sync(RPC_TIMEOUT);
		var l = rq.toList().throwAnyError();
		return l.getFirst().content;
	}

	public class testEndpoint extends InnerClassEndpoint {

		@Override
		public void impl(MessageQueue in) {
			Cout.debugSuperVisible(getFullyQualifiedName() + " received " + in.poll_sync());
		}

		@Override
		public String getDescription() {
			return "just receives a message and tells it on stdout";
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
