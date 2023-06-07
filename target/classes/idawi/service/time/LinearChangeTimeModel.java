package idawi.service.time;

import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;

public class LinearChangeTimeModel<M extends TimeModel> implements TimeModel {
	public M model;
	public Double2DoubleFunction f;

	@Override
	public double getTime() {
		return f.applyAsDouble(model.getTime());
	}
}