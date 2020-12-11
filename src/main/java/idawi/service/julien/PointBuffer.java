package idawi.service.julien;

import java.util.HashMap;

import xycharter.Figure;

public class PointBuffer extends HashMap<String, Figure> {
	private int nbPoints = 0;

	public void add(String metricName, double x, double y) {
		Figure f = get(metricName);

		if (f == null) {
			put(metricName, f = new Figure());
			f.setName(metricName);
		}

		f.addPoint(x, y);
		++nbPoints;
	}

	@Override
	public void clear() {
		super.clear();
		nbPoints = 0;
	}

	public int nbPoints() {
		return nbPoints;
	}
}