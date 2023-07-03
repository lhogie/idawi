package idawi.demo.valentin;

import java.util.Random;

import idawi.Component;
import idawi.RuntimeEngine;
import idawi.transport.UDPDriver;
import toools.collections.Collections;

class NewLinkEvent extends MobilityEvent {

	public NewLinkEvent(double date, Component c, Random prng) {
		super(c, date, prng);
	}

	@Override
	public void run() {
		var a = Collections.pickRandomObject(c.localView().components(), prng);
		var b = a;

		while (b == a) {
			b = Collections.pickRandomObject(c.localView().components(), prng);
		}

		a.need(UDPDriver.class).connectTo(b.need(UDPDriver.class), true);
		RuntimeEngine.offer(new LinkFailEvent(RuntimeEngine.now() + 1, c, prng));
	}
}