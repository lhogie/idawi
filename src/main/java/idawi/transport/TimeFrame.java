package idawi.transport;

import java.io.Serializable;

import idawi.RuntimeEngine;
import toools.SizeOf;

public class TimeFrame implements Serializable, SizeOf {
	private double start, end;

	public TimeFrame(double from) {
		this(from, -1);
	}

	public TimeFrame(double from, double to) {
		this.start = from;
		this.end = to;
	}

	public void close() {
		if (isClosed())
			throw new IllegalStateException("already closed");

		end = RuntimeEngine.now();
	}

	public boolean isClosed() {
		return end > 0;
	}

	public double duration() {
		return isClosed() ? end - start : RuntimeEngine.now() - start;
	}

	@Override
	public long sizeOf() {
		return 8;
	}

	public boolean includes(double time) {
		if (isClosed()) {
			return start <= time && time <= end;
		} else {
			return start <= time;
		}
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