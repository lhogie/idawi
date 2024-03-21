package idawi.service;

import idawi.Component;
import idawi.service.time.Time;
import idawi.service.time.TimeService;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class SimulatedLocationService extends LocationService {
	public double angle;
	public double speed;
	public Time lastMoveDate;

	public SimulatedLocationService(Component node) {
		super(node);
	}

	public void move() {
		Time now = component.service(TimeService.class).now2();

		if (lastMoveDate == null) {
			lastMoveDate = component.service(TimeService.class).now2();
		} else {
			now.ensureSameModel(lastMoveDate);
			double duration = now.value - lastMoveDate.value;
			double distance = speed * duration;
			location.x += distance * Math.cos(angle);
			location.y += distance * Math.sin(angle);
		}
	}

}
