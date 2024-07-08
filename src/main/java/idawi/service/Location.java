package idawi.service;

import java.io.Serializable;
import java.util.Random;

public class Location implements Serializable {
	public double x, y, z;

	public double distanceFrom(Location o) {
		double dx = x - o.x;
		double dy = y - o.y;
		double dz = z - o.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public void random(double max, Random r) {
		x = r.nextDouble(max);
		y = r.nextDouble(max);
		z = r.nextDouble(max);
	}

	@Override
	public String toString() {
		return toString3D();
	}

	public String toString3D() {
		return "(" + x + ", " + y + ", " + z + ")";
	}

	public String toString2D() {
		return "(" + x + ", " + y + ")";
	}

	public Location clone() {
		var clone = new Location();
		clone.x = x;
		clone.y = y;
		clone.z = z;
		return clone;
	}
}