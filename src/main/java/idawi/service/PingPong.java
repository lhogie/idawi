package idawi.service;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Message;
import idawi.MessageList;
import idawi.MessageQueue;
import idawi.MessageQueue.SUFFICIENCY;
import idawi.Operation;
import idawi.Service;
import idawi.To;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class PingPong extends Service {
	public PingPong(Component node) {
		super(node);
	}

	@Operation
	public void ping() {
		// don't need to send anything
		// and EOT will be sent back (acting as a pong)
	}

	@Operation
	public Message traceroute(Message ping) {
		return ping;
	}

	@Override
	public String getFriendlyName() {
		return "ping/pong";
	}

	public Message ping(ComponentInfo target, double timeout) {
		return ping(Set.of(target)).setTimeout(timeout).collect().getOrNull(0);
	}

	public void ping(Set<ComponentInfo> targets, double timeout, Function<ComponentInfo, SUFFICIENCY> pong) {
		ping(targets).forEach(msg -> pong.apply(msg.route.source().component));
	}

	public MessageList ping(Set<ComponentInfo> targets, double timeout) {
		return send(null, new To(targets, PingPong.class, "ping")).setTimeout(timeout).collect();
	}

	public MessageQueue ping(Set<ComponentInfo> targets) {
		return send(null, new To(targets, PingPong.class, "ping"));
	}

	public void discover(double timeout, Consumer<ComponentInfo> found) {
		ping(null).setTimeout(timeout).forEach(newMsg -> SUFFICIENCY.NOT_ENOUGH);
	}
}
