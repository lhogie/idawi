package idawi.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import idawi.Component;
import idawi.messaging.Message;

public class RandomWalk extends RoutingService<RandomWalkParms> {
	private final static Random r = new Random();

	public RandomWalk(Component component) {
		super(component);
	}

	@Override
	public String getAlgoName() {
		return "random";
	}

	@Override
	public RandomWalkParms decode(String s) {
		var p = new RandomWalkParms();
		p.n = Integer.valueOf(s);
		return p;
	}

	@Override
	public void accept(Message msg, RandomWalkParms p) {
		var relays = new ArrayList<>(component.mapService().map.outNeighbors(component.ref()));
		Collections.shuffle(relays);
		var randomRelays = p.n < relays.size() ? relays.subList(0, p.n) : relays;

		for (var t : transports()) {
			t.multicast(msg, randomRelays, this, p);
		}
	}

	@Override
	public RandomWalkParms createDefaultRoutingParms() {
		return new RandomWalkParms();
	}

	@Override
	public TargetComponents naturalTarget(RandomWalkParms parms) {
		return c -> true;
	}
}