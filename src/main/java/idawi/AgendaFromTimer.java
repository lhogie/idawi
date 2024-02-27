package idawi;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

import toools.util.Date;

/**
 * Registers and executes events specified at given points in time. Each event
 * is asynchronously executed in a thread.
 */

public class AgendaFromTimer {
	// an unlimited number of threads is required
	public ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

	public static List<AgendaListener> listeners = new ArrayList<>();

	private List<Event<?>> scheduledEventsQueue = new ArrayList<>();
	private long nbPastEvents = 0;
	private double startTime = -1;
	private double timeAccelarationFactor = 1;
	private Thread controllerThread;
	public Supplier<Boolean> terminated;
	private ArrayBlockingQueue stopQ = new ArrayBlockingQueue(1);

	private Timer timer = new Timer();
	



	public void stop() {
		timer.cancel();
	}

	public synchronized void offer(Event<PointInTime> newEvent) {
		var tt = new TimerTask() {
			
			@Override
			public void run() {
				listeners.forEach(l -> l.eventProcessingStarts(newEvent));
				newEvent.run();
				listeners.forEach(l -> l.eventProcessingCompleted(newEvent));
			}
		};

		timer.schedule(tt, new java.util.Date((long) (1000 * newEvent.when.time)));
		listeners.forEach(l -> l.eventSubmitted(newEvent));
	}

	public void offer(double date, String descr, Runnable r) {
		offer(new Event<PointInTime>(descr, new PointInTime(date)) {

			@Override
			public void run() {
				r.run();
			}
		});
	}

	public long waitForCompletion() throws Throwable {
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
