package idawi.demo.valentin;

import java.util.Random;

import idawi.Component;
import idawi.Idawi;

class LinkFailEvent extends MobilityEvent {

	public LinkFailEvent(double date, Component host, Random prng) {
		super(host, date, prng);
	}

	@Override
	public void run() {
		var l = c.localView().g.pickRandomLink(prng);
		c.localView().g.deactivateLink(l);
		Idawi.agenda.schedule(new NewLinkEvent(Idawi.agenda.now() + 1, c, prng));
	}
}