package idawi;

public class PointInTime implements When, Comparable<PointInTime> {
	public double time;

	public PointInTime(double t) {
		this.time = t;
	}

	@Override
	public boolean test(Event<?> t) {
		return time <= Service.now();
	}

	@Override
	public int compareTo(PointInTime o) {
		return Double.compare(time, o.time);
	}

	@Override
	public String toString() {
		return Instance.prettyTime(time);
	}
}