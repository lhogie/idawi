package idawi.service.julien;

import java.awt.Color;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public class Metric {
	public String name;
	public Unit unit;
	public void setName(String metricName) {
		this.name = metricName;
		
	}
	
	DoubleList x = new DoubleArrayList(), y = new DoubleArrayList();
	private Color color;
	
	public void addPoint(double x, double y) {
		this.x.add(x);
		this.y.add(y);
	}

	public void addPoints(Metric f) {
		f.x.addAll(f.x);
		f.y.addAll(f.y);
	}

	public int getNbPoints() {
		return x.size();
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public double y(int i) {
		return this.y(i);
	}
	public double x(int i) {
		return this.x(i);
	}
}
