package idawi.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import idawi.Component;
import idawi.messaging.Message;

public class RandomWalk extends RoutingService<RandomWalkData> {
	private final static Random r = new Random();

	public RandomWalk(Component component) {
		super(component);
	}

	@Override
	public String getAlgoName() {
		return "random";
	}



	@Override
	public void accept(Message msg, RandomWalkData p) {
		var relays = new ArrayList<>(component.neighbors().infos());
		Collections.shuffle(relays);
		var randomRelays = p.n < relays.size() ? relays.subList(0, p.n) : relays;

		for (var t : transports()) {
			t.multicast(msg, randomRelays, this, p);
		}
	}

	@Override
	public RandomWalkData defaultData() {
		return new RandomWalkData();
	}

	@Override
	public TargetComponents naturalTarget(RandomWalkData parms) {
		return c -> true;
	}
}