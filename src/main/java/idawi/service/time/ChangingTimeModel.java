package idawi.service.time;

public abstract class ChangingTimeModel implements TimeModel {
	TimeModel tm;

	@Override
	public double getTime() {
		return change(tm.getTime());
	}

	protected abstract double change(double time);
}