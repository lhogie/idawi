package idawi.demo.valentin;

import java.util.Random;
import java.util.Set;

import idawi.Component;
import idawi.RuntimeEngine;
import idawi.service.local_view.Network;
import idawi.transport.WiFiDirect;
import toools.collections.Collections;

class NewLinkEvent extends MobilityEvent {

	public NewLinkEvent(double date, Component c, Random prng) {
		super(c, date, prng);
	}

	@Override
	public void run() {
		var a = c.localView().g.pickRandomComponent(prng);
		var b = a;

		while (b == a) {
			b = c.localView().g.pickRandomComponent(prng);
		}

		Network.markLinkActive(a, b, WiFiDirect.class, true, Set.of(a, b));

		RuntimeEngine.offer(new LinkFailEvent(RuntimeEngine.now() + 1, c, prng));
	}
}