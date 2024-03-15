package idawi.service;

import java.util.ArrayList;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.transport.Topologies;

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

	public static void assignRandomLocations(ArrayList<Component> r, double xMax, double yMax, int minDistance) {
		for (var c : r) {
			var ls = c.service(LocationService.class, true);

			while (Topologies.sortByDistanceTo(r, c).get(1).service(LocationService.class).location
					.distanceFrom(ls.location) < minDistance) {
				ls.location.x = xMax * Math.random();
				ls.location.y = yMax * Math.random();
			}
		}
	}

}
