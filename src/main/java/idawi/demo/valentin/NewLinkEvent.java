package idawi.demo.valentin;

import java.util.Collection;
import java.util.Random;

import idawi.Component;
import idawi.RuntimeEngine;
import idawi.Service;
import idawi.transport.UDPDriver;
import toools.collections.Collections;

class NewLinkEvent extends MobilityEvent {
	private Collection<Component> twins;

	public NewLinkEvent(double date, Collection<Component> twins, Random prng) {
		super(date, prng);
		this.twins = twins;
	}

	@Override
	public void run() {
		var a = Collections.pickRandomObject(twins, prng);
		var b = a;

		while (b == a) {
			b = Collections.pickRandomObject(twins, prng);
		}

		a.need(UDPDriver.class).outTo(b);
		RuntimeEngine.offer(new NewLinkEvent(Service.now() + 2, twins, prng));
	}

}