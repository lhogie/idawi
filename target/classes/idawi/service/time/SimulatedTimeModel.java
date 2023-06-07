package idawi.service.time;

public class SimulatedTimeModel implements TimeModel {
	public double time;

	public void setTime(double time) {
		if (time < this.time)
			throw new IllegalArgumentException("cannot get back in time");

		this.time = time;
	}

	@Override
	public double getTime() {
		return time;
	}
}