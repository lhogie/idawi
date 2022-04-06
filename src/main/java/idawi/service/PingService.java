package idawi.service;

import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.InnerOperation;
import idawi.Message;
import idawi.MessageCollector;
import idawi.MessageList;
import idawi.MessageQueue;
import idawi.Service;
import idawi.To;
import toools.io.Cout;

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
			Cout.debugSuperVisible(in.poll_sync());
		}

		@Override
		public String getDescription() {
			return "simply replies to the requester (which stands as the pong)";
		}
	}

	public MessageQueue ping(Set<ComponentDescriptor> targets) {
		return exec(new To(targets).o(PingService.ping.class), true, null).returnQ;
	}

	public MessageQueue ping() {
		return ping((Set<ComponentDescriptor>) null);
	}

	public Message ping(ComponentDescriptor target, double timeout) {
		var r = ping(Set.of(target)).poll_sync(timeout);

		if (r != null && !r.route.source().component.equals(target))
			throw new IllegalStateException("someone else replied to ping!");

		return r;
	}

	public Message ping(ComponentDescriptor target) {
		return ping(target, MessageCollector.DEFAULT_COLLECT_DURATION);
	}

	public MessageList ping(Set<ComponentDescriptor> targets, double timeout, Consumer<Message> realtimeHandler) {
		return ping(targets).recv_sync(timeout, timeout, c -> {
			realtimeHandler.accept(c.messages.last());
			c.stop = c.messages.senders().equals(targets);
		}).messages;
	}

	@Override
	public String getFriendlyName() {
		return "ping/pong";
	}

}
