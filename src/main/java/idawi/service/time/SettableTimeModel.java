package idawi.service.time;

public class SettableTimeModel implements TimeModel {
	double time = 0;

	@Override
	public double getTime() {
		return time;
	}

	public void setTime(double v) {
		if (v < time)
			throw new IllegalArgumentException("can't go backward in time");
		
		this.time += v;
	}
}