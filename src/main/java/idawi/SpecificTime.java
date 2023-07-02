package idawi;

public class SpecificTime implements When, Comparable<SpecificTime> {
	public double time;

	public SpecificTime(double t) {
		this.time = t;
	}

	@Override
	public boolean test(Event<?> t) {
		return time <= Service.now();
	}

	@Override
	public int compareTo(SpecificTime o) {
		return Double.compare(time, o.time);
	}

	@Override
	public String toString() {
		return String.valueOf(time);
	}
}