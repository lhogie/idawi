package idawi.service.time;

public class LinearChangeTimeModel extends ChangingTimeModel {
	double ratio = 1, bias = 0;

	public double change(double time) {
		return time * ratio + bias;
	}
}