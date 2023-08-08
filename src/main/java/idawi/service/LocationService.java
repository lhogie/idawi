package idawi.service;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class LocationService extends Service {
	public Location location = new Location();

	public LocationService(Component node) {
		super(node);
	}

	public class location extends TypedInnerClassEndpoint {

		public Location get() {
			return location;
		}

		@Override
		public String getDescription() {
			return "get the location of this component";
		}
	}

}
