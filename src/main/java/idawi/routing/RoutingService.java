package idawi.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import idawi.Component;
import idawi.Endpoint;
import idawi.EndpointParameterList;
import idawi.InnerClassEndpoint;
import idawi.RemotelyRunningEndpoint;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import idawi.messaging.RoutingStrategy;
import idawi.service.PingService;
import idawi.service.PingService.ping;
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

	private RemotelyRunningEndpoint exec(Consumer<Message> privateCustomizer, Consumer<Message> userCustomizer) {
		P defaultData = defaultData();
		var returnQ = createUniqueQueue("return-");

		var msg = new Message();
		msg.initialRoutingStrategy = new RoutingStrategy(this, defaultData);
		msg.replyTo = new QueueAddress(ComponentMatcher.unicast(component), getClass(), returnQ.name);
		privateCustomizer.accept(msg);

		if (userCustomizer != null) {
			userCustomizer.accept(msg);
		}

		accept(msg, defaultData);
		return new RemotelyRunningEndpoint(msg.qAddr, returnQ);
	}

	public RemotelyRunningEndpoint exec(ComponentMatcher t, Class<? extends Service> s, Class<? extends Endpoint> e,
			Object initialInputData, Consumer<Message> msgCustomizer) {
		return exec(msg -> {
			msg.qAddr = new QueueAddress(t, s, e.getSimpleName() + "@" + now());
			msg.autoCreateQueue = true;
			msg.deleteQueueAfterCompletion = true;
			msg.endpointID = e;
			msg.content = initialInputData;
		}, msgCustomizer);
	}

	public RemotelyRunningEndpoint exec(Component c, Class<? extends Service> service,
			Class<? extends InnerClassEndpoint> e, Object initialInputData, Consumer<Message> msgCustomizer) {
		return exec(ComponentMatcher.unicast(c), service, e, initialInputData, msgCustomizer);
	}

	public RemotelyRunningEndpoint send(Object content, QueueAddress dest) {
		return send(content, dest, msg -> msg.eot = false);
	}

	public RemotelyRunningEndpoint send(Object content, QueueAddress dest, Consumer<Message> msgCustomizer) {
		return exec(msg -> {
			msg.qAddr = dest;
			msg.content = content;
			msg.endpointID = deliverToQueue.class;
		}, msgCustomizer);
	}

	public static double RPC_TIMEOUT = 5;

	public Object exec_rpc(Component to, Class<? extends Service> service, Class<? extends InnerClassEndpoint> o,
			Object parms) {
		return exec(to, service, o, new EndpointParameterList(parms), null).returnQ.poll_sync(RPC_TIMEOUT)
				.throwIfError().content;
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

	public MessageQueue ping(ComponentMatcher target, P p) {
		return exec(target, PingService.class, ping.class, "foobar",
				msg -> msg.initialRoutingStrategy.parms = p).returnQ;
	}

	public MessageQueue ping(Collection<Component> targets, P p) {
		return ping(ComponentMatcher.multicast(targets), p);
	}

	public MessageQueue ping(Collection<Component> targets) {
		return ping(ComponentMatcher.multicast(targets), defaultData());
	}

	public Message ping(Component target) {
		var pong = ping(ComponentMatcher.unicast(target), defaultData()).poll_sync();

		if (pong == null) {
			return null;
		} else {
			return pong.throwIfError();
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
