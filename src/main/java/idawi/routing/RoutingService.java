package idawi.routing;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import idawi.Component;
import idawi.InnerClassOperation;
import idawi.OperationParameterList;
import idawi.RemotelyRunningOperation;
import idawi.Service;
import idawi.TypedInnerClassOperation;
import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import idawi.transport.TransportService;
import toools.util.Date;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public abstract class RoutingService<P extends RoutingData> extends Service implements BiConsumer<Message, P> {
	protected final AtomicLong nbMsgReceived = new AtomicLong();

	public RoutingService(Component node) {
		super(node);
		registerOperation(new ping());
		registerOperation(new suggestParms());
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

	protected P convert(RoutingData p) {
		return (P) p;
	}

	public abstract TargetComponents naturalTarget(P parms);

	public abstract String getAlgoName();

	public abstract List<P> dataSuggestions();

	public P defaultData() {
		return dataSuggestions().get(0);
	}

	public void send(Object value, TargetComponents r, Class<? extends Service> s, String queueID) {
//		Cout.debugSuperVisible("send " + value);

		var dest = new MessageQDestination();
		dest.queueID = queueID;
		dest.service = s;
		dest.componentTarget = r;

		send(value, dest, defaultData());
	}

	public void send(Object value, Destination dest, P parms) {
		accept(new Message(dest, value), parms);
	}

	public void send(Object value, MessageQDestination dest) {
		send(value, dest, defaultData());
	}

	public void send(String value, RemotelyRunningOperation o) {
		send(value, o.getOperationInputQueueDestination());
	}

	public RemotelyRunningOperation exec(Class<? extends Service> target,Class<? extends InnerClassOperation> o, P parms, TargetComponents re,
			boolean returnQ, Object initialInputData) {
		return exec(target, o, parms, re, returnQ ? createAutoQueue("returnQ") : null, initialInputData);
	}
	public RemotelyRunningOperation exec(Class<? extends InnerClassOperation> o, P parms, TargetComponents re,
			boolean returnQ, Object initialInputData) {
		return exec(o, parms, re, returnQ ? createAutoQueue("returnQ") : null, initialInputData);
	}

	public RemotelyRunningOperation exec(Component to, Class<? extends InnerClassOperation> o,
			Object initialInputData) {
		return exec(o, null, new TargetComponents.Unicast(to), true, initialInputData);
	}

	public RemotelyRunningOperation exec(Component to, Class<? extends InnerClassOperation> o, P parms,
			Object initialInputData) {
		return exec(o, parms, new TargetComponents.Unicast(to), true, initialInputData);
	}

	public RemotelyRunningOperation exec(Class<? extends InnerClassOperation> o, Object initialInputData) {
		var parms = defaultData();
		return exec(o, parms, naturalTarget(parms), true, initialInputData);
	}

	public RemotelyRunningOperation exec(Class<? extends InnerClassOperation> o, P parms, TargetComponents re,
			MessageQueue returnQ, Object initialInputData) {
		return exec(InnerClassOperation.serviceClass(o), o, parms, re, returnQ, initialInputData);
	}

	public RemotelyRunningOperation exec(Class<? extends Service> target, Class<? extends InnerClassOperation> o, P parms, TargetComponents re,
			MessageQueue returnQ, Object initialInputData) {
		var dest = new MessageODestination();
		dest.invocationDate = Date.timeNs();
		dest.operationID = o;
		dest.componentTarget = re;
		dest.targetService = target; 
		var r = new RemotelyRunningOperation();

		if (returnQ != null) {
			r.returnQ = returnQ;
			dest.replyTo = new MessageQDestination();
			dest.replyTo.componentTarget = new TargetComponents.Unicast(component);
			dest.replyTo.queueID = r.returnQ.name;
			dest.replyTo.service = r.returnQ.service.getClass();
		}

		send(initialInputData, dest, parms);
		r.destination = dest;
		return r;
	}

	public Object exec_rpc(Component to, Class<? extends InnerClassOperation> o, Object parms) {
		var rec = new TargetComponents.Unicast(to);
		var rq = exec(o, defaultData(), rec, createAutoQueue("returnQ"), new OperationParameterList(parms)).returnQ;
		return rq.toList().throwAnyError_Runtime().getContentOrNull(0);
	}

	public class ping extends InnerClassOperation {

		@Override
		public String getDescription() {
			return "just sends EOT to the requester (which stands as the pong)";
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
//			Cout.debugSuperVisible("PING");
			var m = in.poll_sync();
			// sends back the ping message to the caller
			reply(m, m);
		}
	}

	public class suggestParms extends TypedInnerClassOperation {

		@Override
		public String getDescription() {
			return "suggestParms";
		}

		public List<String> impl() {
			return dataSuggestions().stream().map(d -> d.toURLElement()).toList();
		}
	}

	public MessageQueue ping(Set<Component> targets, P parms) {
		return exec(ping.class, parms, new TargetComponents.Multicast(targets), true, "ping").returnQ;
	}

	public MessageQueue ping(Set<Component> targets) {
		return ping(targets, defaultData());
	}

	public MessageQueue ping(Component target) {
		return ping(Set.of(target), defaultData());
	}

	public MessageQueue ping(P parms) {
		return exec(ping.class, parms, naturalTarget(parms), true, "ping").returnQ;
	}

	public MessageQueue ping() {
		return ping(defaultData());
	}

}
