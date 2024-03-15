package idawi;

import toools.util.Date;

public class PointInTime implements When, Comparable<PointInTime> {
	public double time;

	public PointInTime(double t) {
		this.time = t;
	}

	@Override
	public boolean test(Event<?> t) {
		return time <= Idawi.agenda.now();
	}

	@Override
	public int compareTo(PointInTime o) {
		return Double.compare(time, o.time);
	}

	@Override
	public String toString() {
		return Date.prettyTime(time);
	}
}