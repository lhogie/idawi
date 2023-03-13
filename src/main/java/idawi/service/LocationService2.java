package idawi.service;

import java.io.Serializable;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassOperation;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class LocationService2 extends Service {

	public static class Location implements Serializable {
		public double x, y;

		public double distanceFrom(Location o) {
			double dx = x - o.x;
			double dy = y - o.y;
			return Math.sqrt(dx * dx + dy * dy);
		}

		@Override
		public String toString() {
			return "(" + x + ", " + y + ")";
		}
	}

	public Location location = new Location();

	public LocationService2(Component node) {
		super(node);
	}

	public Location getLocation() {
		return location;
	}

	public class location extends TypedInnerClassOperation {

		public Location get() {
			return getLocation();
		}

		@Override
		public String getDescription() {
			return "get the location of this component";
		}
	}

}
