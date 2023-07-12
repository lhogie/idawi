package idawi.demo.valentin;

import java.util.Random;

import idawi.Component;
import idawi.Event;
import idawi.PointInTime;

abstract class MobilityEvent extends Event<PointInTime> {
	protected Random prng;
	protected Component c;

	public MobilityEvent(Component c, double date, Random prng) {
		super(new PointInTime(date));
		this.prng = prng;
		this.c = c;
	}
}