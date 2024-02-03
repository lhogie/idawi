package idawi.demo.valentin;

import java.util.Random;

import idawi.Component;
import idawi.Idawi;
import idawi.Agenda;
import toools.collections.Collections;

class LinkFailEvent extends MobilityEvent {

	public LinkFailEvent(double date, Component host, Random prng) {
		super(host, date, prng);
	}

	@Override
	public void run() {
		var l = c.localView().g.pickRandomLink(prng);
		c.localView().g.deactivateLink(l);
		Idawi.agenda.offer(new NewLinkEvent(Agenda.now() + 1, c, prng));
	}
}