package idawi.service.time;

public class ControlledTimeModel implements TimeModel {
	double time = 0;

	@Override
	public double getTime() {
		return time;
	}
}