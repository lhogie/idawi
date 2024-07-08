package idawi.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.SupplierEndPoint;
import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import idawi.service.PingService;
import idawi.service.PingService.ping;
import idawi.transport.TransportService;
import toools.io.Cout;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public abstract class RoutingService<P extends RoutingParameters> extends Service implements Consumer<Message<?>> {
	protected final AtomicLong nbMsgReceived = new AtomicLong();
	public final List<RoutingListener> listeners = new ArrayList<>();

	public long nbMessagesInitiated;

	public RoutingService(Component node) {
		super(node);
	}

	@Override
	public void accept(Message<?> msg) {
//		accept(msg);
		++nbMessagesInitiated;
		acceptImpl(msg, (P) msg.routingStrategy.parms);
	}

	protected abstract void acceptImpl(Message<?> msg, P p);

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

	public class testEndpoint extends InnerClassEndpoint<Void, Void> {

		@Override
		public void impl(MessageQueue in) {
			Cout.debugSuperVisible(getFullyQualifiedName() + " received " + in.poll_sync());
		}

		@Override
		public String getDescription() {
			return "just receives a message and tells it on stdout";
		}
	}

	public class suggestParms extends SupplierEndPoint<List<String>> {

		@Override
		public List<String> get() {
			return dataSuggestions().stream().map(d -> d.toURLElement()).toList();
		}

		@Override
		protected String r() {
			return "suggest parms";
		}
	}

	public MessageQueue ping(ComponentMatcher target, P p) {
		return exec(target, PingService.class, ping.class, msg -> {
			msg.routingStrategy.parms = p;
			msg.content = "foobar";
		}).returnQ;
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
