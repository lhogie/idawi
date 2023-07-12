package idawi.demo.valentin;

import java.util.Random;

import idawi.Component;
import idawi.RuntimeEngine;
import toools.collections.Collections;

class LinkFailEvent extends MobilityEvent {

	public LinkFailEvent(double date, Component host, Random prng) {
		super(host, date, prng);
	}

	@Override
	public void run() {
		var l = Collections.pickRandomObject(c.localView().links().set, prng);
		c.localView().g.deactivateLink(l);
		RuntimeEngine.offer(new NewLinkEvent(RuntimeEngine.now() + 1, c, prng));
	}
}