package idawi.service.time;

public class SettableTimeModel implements TimeModel {
	double time = 0;

	@Override
	public double getTime() {
		return time;
	}
}