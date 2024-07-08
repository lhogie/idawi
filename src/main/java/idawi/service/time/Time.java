package idawi.service.time;

import java.io.Serializable;

public class Time implements Serializable {
	public double value;
	public TimeModel model;

	public Time(double time, TimeModel model) {
		this.value = time;
		this.model = model;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		Time t = (Time) obj;
		return t.model.getClass() == model.getClass() && t.value == t.value;
	}

	@Override
	public String toString() {
		return value + " according to " + model.getClass();
	}

	public void ensureSameModel(Time t) {
		if (t.model.getClass() != model.getClass())
			throw new IllegalArgumentException("times have different models");
	}
}
