package idawi.demo.valentin;

import java.util.Random;

import idawi.Component;
import idawi.RuntimeEngine;
import idawi.Service;
import toools.collections.Collections;

class LinkFailEvent extends MobilityEvent {
	private Component host;

	public LinkFailEvent(double date, Component host, Random prng) {
		super(date, prng);
		this.host = host;
	}

	@Override
	public void run() {
		var l = Collections.pickRandomObject(host.localView().links().set, prng);
		host.localView().links().remove(l);
		RuntimeEngine.eventQueue.offer(new LinkFailEvent(Service.now() + 2, host, prng));
	}
}