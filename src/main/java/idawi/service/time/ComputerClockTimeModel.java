package idawi.service.time;

import toools.util.Date;

public class ComputerClockTimeModel implements TimeModel {
	@Override
	public double getTime() {
		return Date.time();
	}
}