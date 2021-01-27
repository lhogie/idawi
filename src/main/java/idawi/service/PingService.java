package idawi.service;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.IdawiExposed;
import idawi.Message;
import idawi.MessageList;
import idawi.MessageQueue;
import idawi.MessageQueue.SUFFICIENCY;
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
	}

	@IdawiExposed
	public static class ping
	{
		private void ping() {
		}
	}

	@IdawiExposed
	private Message traceroute(Message ping) {
		return ping;
	}

	@Override
	public String getFriendlyName() {
		return "ping/pong";
	}

	public List<Route> traceroute(Set<ComponentDescriptor> targets, double timeout) throws Throwable {
		return send(null, new To(targets, PingService.class, "traceroute")).setTimeout(timeout).collect()
				.throwAnyError().resultMessages().contents().stream().map(r -> ((Message) r).route)
				.collect(Collectors.toList());
	}

	public Message ping(ComponentDescriptor target, double timeout) {
		return ping(Set.of(target)).setTimeout(timeout).collect().getOrNull(0);
	}

	public void ping(Set<ComponentDescriptor> targets, double timeout,
			Function<ComponentDescriptor, SUFFICIENCY> pong) {
		ping(targets).forEach(msg -> pong.apply(msg.route.source().component));
	}

	public MessageList ping(Set<ComponentDescriptor> targets, double timeout) {
		return send(null, new To(targets, PingService.class, "ping")).setTimeout(timeout).collect();
	}

	public MessageQueue ping(Set<ComponentDescriptor> targets) {
		return send(null, new To(targets, PingService.class, "ping"));
	}

	public MessageQueue pingAround() {
		return ping(null);
	}

	public void pingAround(double timeout, Consumer<ComponentDescriptor> found) {
		ping(null).setTimeout(timeout).forEach(newMsg -> {
			newMsg.route.forEach(e -> found.accept(e.component));
			return SUFFICIENCY.NOT_ENOUGH;
		});
	}
}
