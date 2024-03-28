package idawi;

public class TooLateException extends Exception {

	final public double delay;
	final public Event e;

	public TooLateException(Event<PointInTime> e, double delay) {
		this.e = e;
		this.delay = delay;
	}

}
