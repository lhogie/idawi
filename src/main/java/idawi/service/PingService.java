package idawi.service;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.InnerOperation;
import idawi.Message;
import idawi.MessageList;
import idawi.MessageQueue;
import idawi.MessageQueue.Enough;
import idawi.Route;
import idawi.Service;
import idawi.To;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class PingService extends Service {
	public PingService(Component node) {
		super(node);
		registerOperation(new ping());
	}

	public class ping extends InnerOperation {
		@Override
		public void exec(MessageQueue in) throws Throwable {
			// do nothing, the EOT message will go automatically
		}

		@Override
		public String getDescription() {
			return "simply replies to the requester (which stands as the pong)";
		}
	}

	public static MessageQueue ping(Service from, Set<ComponentDescriptor> targets) {
		return from.exec(new To(targets).o(PingService.ping.class), true, null).returnQ;
	}

	public static MessageQueue ping(Service from) {
		return ping(from, null);
	}

	public static Message ping(Service from, ComponentDescriptor target, double timeout) {
		return ping(from, Set.of(target)).setMaxWaitTimeS(timeout).collect().getOrNull(0);
	}

	public static void ping(Service from, Set<ComponentDescriptor> targets, double timeout,
			Function<ComponentDescriptor, Enough> pong) {
		ping(from, targets).forEach(msg -> pong.apply(msg.route.source().component));
	}

	public static MessageList ping(Service from, Set<ComponentDescriptor> targets, double timeout) {
		return ping(from, targets).setMaxWaitTimeS(timeout).collect();
	}

	public static void ping(Service from, double timeout, Consumer<Message> found) {
		ping(from, null).setMaxWaitTimeS(timeout).forEach(newMsg -> {
			found.accept(newMsg);
			return Enough.no;
		});
	}

	public class traceroute extends InnerOperation {
		@Override
		public void exec(MessageQueue in) throws Throwable {
			var msg = in.get_blocking();
			send(msg.route, msg.replyTo);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public static List<Route> traceroute(Service from, Set<ComponentDescriptor> targets, double timeout)
			throws Throwable {
		return (List<Route>) (List<?>) from.exec(new To(targets).o(PingService.traceroute.class), true, null).returnQ
				.setMaxWaitTimeS(timeout).collect().throwAnyError().resultMessages().contents().stream()
				.collect(Collectors.toList());
	}

	@Override
	public String getFriendlyName() {
		return "ping/pong";
	}

}
