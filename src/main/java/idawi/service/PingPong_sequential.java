package idawi.service;

import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Message;
import idawi.MessageList;
import idawi.Service;
import idawi.To;
import toools.thread.Q;
import toools.util.Date;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class PingPong_sequential extends Service {
	public PingPong_sequential(Component peer) {
		super(peer);

		newThread(() -> {
			Q<Message> q = getQueue(null);

			while (true) {
				Message msg = q.get_blocking();
				send(msg, msg.replyTo, null);
			}
		});
	}

	@Override
	public String getFriendlyName() {
		return "ping agents (imperative version)";
	}
	
	public double ping(ComponentInfo peer, long timeoutMs) {
		To to = new To();
		to.notYetReachedExplicitRecipients = Set.of(peer);
		to.service = id;
		MessageList response = send("ping", to).setTimeout(timeoutMs/1000d).collect();

		if (response.isEmpty()) {
			return - 1;
		}

		return Date.time() - response.last().route.last().emissionDate;
	}
}
