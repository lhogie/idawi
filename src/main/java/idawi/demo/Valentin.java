package idawi.demo;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import idawi.Component;
import idawi.RuntimeEngine;
import idawi.Service;
import idawi.Service.Event;
import idawi.Service.SpecificTime;
import idawi.service.DigitalTwinService;
import idawi.transport.Topologies;
import idawi.transport.UDPDriver;
import toools.collections.Collections;

public class Valentin {
	static Component c = new Component("root");
	static List<Component> twins = Component.createNComponent("c", 100);
	static Random prng = new Random();
	static double mobilityEventPeriodicity = 1;

	static class LinkFailEvent extends Event<SpecificTime> {
		public LinkFailEvent(double date) {
			super(new SpecificTime(date));
		}

		@Override
		public void run() {
			var l = Collections.pickRandomObject(c.localView().links().set, prng);
			c.localView().links().remove(l);
			RuntimeEngine.eventQueue.offer(new NewLinkEvent(Service.now() + mobilityEventPeriodicity));
		}
	}

	static class NewLinkEvent extends Event<SpecificTime> {
		public NewLinkEvent(double date) {
			super(new SpecificTime(date));
		}

		@Override
		public void run() {
			var a = Collections.pickRandomObject(twins, prng);
			var b = a;

			while (b == a) {
				b = Collections.pickRandomObject(twins, prng);
			}

			a.need(UDPDriver.class).outTo(b);

			RuntimeEngine.eventQueue.offer(new LinkFailEvent(Service.now() + mobilityEventPeriodicity));
		}

	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {

		for (var twin : twins) {
			new DigitalTwinService(twin, c.localView());
		}

		Topologies.chainRandomly(twins, 3, prng, UDPDriver.class, true);

		RuntimeEngine.simulationMode();

		RuntimeEngine.eventQueue.offer(new NewLinkEvent(Service.now()));
	}
}
