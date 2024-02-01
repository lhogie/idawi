package idawi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import toools.util.Date;

public class Agenda {
	public ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
//	public static AtomicDouble simulatedTime;
	private PriorityBlockingQueue<Event<PointInTime>> eventQueue = new PriorityBlockingQueue<>(100,
			(a, b) -> a.when.compareTo(b.when));

	private static ArrayBlockingQueue stopQ = new ArrayBlockingQueue(1);

	public final static List<AgendaListener> listeners = new ArrayList<>();

	private static List<Event<?>> scheduledEventsQueue = new ArrayList<>();
	static long nbPastEvents = 0;
	public static double startTime = -1;
	public static double timeAcceleartionFactor = 1;
	private static Thread controllerThread;
	public static Supplier<Boolean> terminated;

	public void startInThread() {
		threadPool.submit(() -> {
			try {
				processEventQueue();
			} catch (Throwable err) {
				err.printStackTrace();
			}
		});

		IntStream.range(0, 6).mapToObj(i -> new ArrayList<Integer>()).toList();

		var l = new ArrayList<Integer>();
		IntStream.range(0, 6).mapToObj(i -> l).toList();

		l.stream().map(i -> i * 6).toList();

	}

	public long processEventQueue() throws Throwable {
		startTime = Date.time();
		controllerThread = Thread.currentThread();
		listeners.forEach(l -> l.starting());

		while (terminated == null || !terminated.get()) {
			var e = grabCloserEvent();

			if (e == null) {
				// unblock those waiting for completion
				stopQ.offer("");
			}

			listeners.forEach(l -> l.newEventScheduledForExecution(e));

			// if (!threadPool.isShutdown())
			threadPool.submit(new Runnable() {
				Event<?> _e = e;
				{
					scheduledEventsQueue.add(e);
				}

				@Override
				public void run() {
					try {
						listeners.forEach(l -> l.eventProcessingStarts(_e));
						_e.run();
						++nbPastEvents;
						scheduledEventsQueue.remove(_e);
						listeners.forEach(l -> l.eventProcessingCompleted(_e));
					} catch (Throwable err) {
						err.printStackTrace();
						stopQ.offer(err);
						terminated = () -> true;
					}
				}
			});
		}

		threadPool.shutdown();
		controllerThread = null;
		Thread.currentThread().interrupt();
		listeners.forEach(l -> l.terminating(nbPastEvents));
		return blockUntilSimulationHasCompleted();
	}

	private synchronized Event<PointInTime> grabCloserEvent() {
		while (controllerThread != null) {
			var e = eventQueue.peek();

			try {
				if (e == null) {
					listeners.forEach(l -> l.sleeping(Double.MAX_VALUE, null));
					Thread.sleep(Long.MAX_VALUE);
				} else {
					double wait = e.when.time - now();

					if (wait > 0) {
						listeners.forEach(l -> l.sleeping(wait, e));
						Thread.sleep((long) (1000L * wait));
					}

					if (wait < 0) {
//						System.err.println("go backward: " + wait);
					}

					return eventQueue.poll();
				}
			} catch (InterruptedException interupt) {
				listeners.forEach(l -> l.interrupted());
			}
		}

		return null;
	}

	public static double now() {
		if (startTime == -1)
			return 0;

		return (Date.time() - startTime) * timeAcceleartionFactor;
	}

	public synchronized void offer(Event<PointInTime> newEvent) {
		var needInterrupt = eventQueue.isEmpty() || newEvent.when.time < eventQueue.peek().when.time;
		eventQueue.offer(newEvent);
		listeners.forEach(l -> l.eventSubmitted(newEvent));

		if (controllerThread != null && needInterrupt) {
			controllerThread.interrupt();
		}
	}

	public void offer(double date, String descr, Runnable r) {
		offer(new Event<PointInTime>(descr, new PointInTime(date)) {

			@Override
			public void run() {
				r.run();
			}
		});
	}

	public long blockUntilSimulationHasCompleted() throws Throwable {
		try {
			// stopPlatformThreads();
			var o = stopQ.take();

			if (o instanceof Throwable) {
				throw (Throwable) o;
			} else {
				return nbPastEvents;
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}
}
