package idawi.demo;

import java.io.IOException;
import java.util.Random;

import idawi.Component;
import idawi.Service;
import idawi.Service.Event;
import idawi.Service.SpecificTime;
import idawi.transport.SharedMemoryTransport;
import toools.collections.Collections;

public class Valentin {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		var components = Component.createNComponent("c", 100);
		var prng = new Random();

		SharedMemoryTransport.chainRandomly(components, 3, prng, SharedMemoryTransport.class, true);

		double mobilityEventPeriodicity = 1;
		Service.simulationMode();
		
		class MobilityEvent extends Event<SpecificTime> {

			public MobilityEvent(double d) {
				when.time = d;
			}

			@Override
			public void run() {
				if (prng.nextBoolean()) {
					removeRandomLink();
				}else {
					createRandomLink();
				}

				Service.eventQueue.offer(new MobilityEvent(Service.now() + mobilityEventPeriodicity));
			}

			private void createRandomLink() {
				var a = Collections.pickRandomObject(components, prng);
				var b = Collections.pickRandomObject(components, prng);
				a.need(SharedMemoryTransport.class).outTo(b);
			}

			private void removeRandomLink() {
				while (true) {
					var rc = Collections.pickRandomObject(components, prng);
					rc.need(SharedMemoryTransport.class).
				}
				
				var l1 = Collections.pickRandomObject(rc.localView().links(), prng);
				rc.localView().links().remove(l1);
			}
		}

		Service.eventQueue.offer(new MobilityEvent(Service.now()));
	}
}
