package idawi;

import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.AtomicDouble;

import idawi.Service.Event;
import idawi.Service.SpecificTime;
import toools.util.Date;

public class RuntimeEngine {
	public static ExecutorService threadPool = Executors.newCachedThreadPool();
	public static AtomicDouble simulatedTime;
	public static PriorityQueue<Event<SpecificTime>> eventQueue = new PriorityQueue<>(
			(a, b) -> a.when.compareTo(b.when));

	public static void simulationMode() {
		simulatedTime = new AtomicDouble(0);
	}

	static {
		threadPool.submit(() -> {
			var e = eventQueue.poll();
			simulatedTime.set(e.when.time);
			threadPool.submit(e);
		});
	}

	public static double now() {
		return simulatedTime == null ? Date.time() : simulatedTime.get();
		// var ts = lookup(TimeService.class);
		// return ts == null ? Date.time() : ts.now();
	}

	public static void stopPlatformThreads() {
		threadPool.shutdown();
	}

}
