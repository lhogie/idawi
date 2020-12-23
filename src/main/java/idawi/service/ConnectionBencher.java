package idawi.service;

import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.MessageList;
import idawi.Service;
import idawi.To;
import toools.util.Date;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class ConnectionBencher extends Service {
	public ConnectionBencher(Component peer) {
		super(peer);
		registerOperation(null, (msg, out) -> {
		});
	}

	@Override
	public String getFriendlyName() {
		return "bench connection speed";
	}

	public double benchLinkTo2(ComponentInfo peer, double timeout) {
		To to = new To();
		to.notYetReachedExplicitRecipients = Set.of(peer);
		to.service = id;
		MessageList msg = send(null, to).collect();
		return Date.time() - msg.last().route.last().emissionDate;
	}
}
