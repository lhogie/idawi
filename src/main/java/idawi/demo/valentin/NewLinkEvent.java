package idawi.demo.valentin;

import java.util.Random;
import java.util.Set;

import idawi.Component;
import idawi.Idawi;
import idawi.Agenda;
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

		Idawi.agenda.schedule(new LinkFailEvent(Idawi.agenda.now() + 1, c, prng));
	}
}