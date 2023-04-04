package idawi.service;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassOperation;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class LocationService extends Service {
	public Location location = new Location();

	public LocationService(Component node) {
		super(node);
		registerOperation(new location());
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
