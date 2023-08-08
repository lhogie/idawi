package idawi.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
		var relays = component.outLinks();
		Collections.shuffle(relays);
		var randomRelays = p.n < relays.size() ? relays.subList(0, p.n) : relays;

		for (var t : transports()) {
			t.send(msg, randomRelays, this, p);
		}
	}

	@Override
	public List<RandomWalkData> dataSuggestions() {
		var l = new ArrayList<RandomWalkData>();
		l.add(new RandomWalkData());
		return l;
	}

	@Override
	public ComponentMatcher naturalTarget(RandomWalkData parms) {
		return ComponentMatcher.all;
	}
}