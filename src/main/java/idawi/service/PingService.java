package idawi.service;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.ComponentAddress;
import idawi.ComponentDescriptor;
import idawi.InnerClassOperation;
import idawi.Message;
import idawi.MessageList;
import idawi.MessageQueue;
import idawi.MessageQueue.SUFFICIENCY;
import idawi.Route;
import idawi.Service;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class PingService extends Service {
	public PingService(Component node) {
		super(node);
	}

	public class ping extends InnerClassOperation{
		@Override
		public void exec(MessageQueue in) throws Throwable {
		}

		@Override
		public String getDescription() {
			return "simply replies to the requester";
		}
	}


	public static MessageQueue ping(Service from, Set<ComponentDescriptor> targets) {
		return from.start(new ComponentAddress(targets).o(PingService.ping.class), true, null).returnQ;
	}

	public static MessageQueue ping(Service from) {
		return ping(from, null);
	}

	public static Message ping(Service from, ComponentDescriptor target, double timeout) {
		return ping(from, Set.of(target)).setTimeout(timeout).collect().getOrNull(0);
	}

	public static void ping(Service from, Set<ComponentDescriptor> targets, double timeout,
			Function<ComponentDescriptor, SUFFICIENCY> pong) {
		ping(from, targets).forEach(msg -> pong.apply(msg.route.source().component));
	}

	public static MessageList ping(Service from, Set<ComponentDescriptor> targets, double timeout) {
		return ping(from, targets).setTimeout(timeout).collect();
	}

	public static void ping(Service from, double timeout, Consumer<Message> found) {
		ping(from, null).setTimeout(timeout).forEach(newMsg -> {
			found.accept(newMsg);
			return SUFFICIENCY.NOT_ENOUGH;
		});
	}

	public class traceroute extends InnerClassOperation{
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
		return (List<Route>) (List<?>) from.start(new ComponentAddress(targets).o(PingService.traceroute.class), true,
				null).returnQ.setTimeout(timeout).collect().throwAnyError().resultMessages().contents().stream()
						.collect(Collectors.toList());
	}

	@Override
	public String getFriendlyName() {
		return "ping/pong";
	}

}
