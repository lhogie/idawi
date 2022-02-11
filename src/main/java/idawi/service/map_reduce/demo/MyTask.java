package idawi.service.map_reduce.demo;

import java.util.function.Consumer;

import idawi.service.map_reduce.Task;

// that's the task we'll send to workers
class MyTask extends Task<Integer> {
	double minDuration = 0, maxDuration = 0;

	@Override
	public Integer compute(Consumer output) {
		// 0.1 chances that this task fails
		if (Math.random() < 0) {
			output.accept("I'm feeling bad");
			throw new Error();
		}

		// Threads.sleep((maxDuration - minDuration) * Math.random() + minDuration);
		return 1;
	}
}