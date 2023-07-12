package idawi.transport;

import java.io.Serializable;

import idawi.RuntimeEngine;
import toools.SizeOf;

public class TimeFrame implements Serializable, SizeOf {
	private double start, end;
	public static double TIMEOUT = 2;

	public TimeFrame(double from) {
		this(from, from);
	}

	public TimeFrame(double from, double to) {
		this.start = from;
		this.end = to;
	}

	public boolean isOver() {
		return RuntimeEngine.now() - end > TIMEOUT;
	}

	public double duration() {
		return end - start;
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
			throw new IllegalStateException();

		this.end = t;
	}
}