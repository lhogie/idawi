package idawi.transport;

import java.io.Serializable;

import idawi.Idawi;
import toools.SizeOf;

public class TimeFrame implements Serializable, SizeOf {
	private double start, end;
	public static double TIMEOUT = 2;

	public TimeFrame(double from) {
		this(from, Double.MAX_VALUE);
	}

	public TimeFrame(double from, double to) {
		if (to <= from)
			throw new IllegalStateException(to + " > " + from);

		this.start = from;
		this.end = to;
	}

	public boolean isClosed() {
		return end < Double.MAX_VALUE;
	}

	public double duration() {
		return isClosed() ? end - start : Idawi.agenda.now() - start;
	}

	@Override
	public long sizeOf() {
		return 8;
	}

	public boolean includes(double time) {
		return start <= time && time <= end;
	}

	public double end() {
		return end;
	}

	public double start() {
		return start;
	}

	public void end(double t) {
		if (t <= start)
			throw new IllegalStateException(t + " > " + start);

		this.end = t;
	}
}