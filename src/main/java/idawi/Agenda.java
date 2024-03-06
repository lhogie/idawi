package idawi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

import toools.io.Cout;
import toools.util.Date;

/**
 * Registers and executes events specified at given points in time. Each event
 * is asynchronously executed in a thread.
 */

public class Agenda {
	// an unlimited number of threads is required
	public ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
//	public static AtomicDouble simulatedTime;
	private PriorityBlockingQueue<Event<PointInTime>> eventQueue = new PriorityBlockingQueue<>(100,
			(a, b) -> a.when.compareTo(b.when));

	public static List<AgendaListener> listeners = new ArrayList<>();

	private List<Event<?>> scheduledEventsQueue = new ArrayList<>();
	private long nbPastEvents = 0;
	private double startTime = -1;
	private double timeAccelarationFactor = 1;
	private Thread controllerThread;
	private Supplier<Boolean> terminationCondition;
	private ArrayBlockingQueue stopQ = new ArrayBlockingQueue(1);

	
	public synchronized void start() {
		if (isStarted())
			throw new IllegalStateException("already started");

		startTime = Date.time();

		threadPool.submit(() -> {
			try {
				processEventQueue();
			} catch (Throwable err) {
				err.printStackTrace();
			}
		});
	}

	public synchronized boolean isStarted() {
		return startTime > 0;
	}

	private void processEventQueue() throws Throwable {
		controllerThread = Thread.currentThread();
		listeners.forEach(l -> l.starting());

		while (terminationCondition == null || !terminationCondition.get()) {

			try {
				final var candidateEvent = eventQueue.peek();

				if (candidateEvent == null) {
					Thread.sleep(Long.MAX_VALUE);
				} else {
					double wait = candidateEvent.when.time - now();

					if (wait > 0) {
						listeners.forEach(l -> l.sleeping(wait, candidateEvent));
						Thread.sleep((long) (1000L * wait));
					} else {
						var e = eventQueue.poll();

						listeners.forEach(l -> l.newEventInThreadPool(candidateEvent));

						// if (!threadPool.isShutdown())
						threadPool.submit(new Runnable() {
							final Event<?> _e = e;

							{
								scheduledEventsQueue.add(candidateEvent);
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
									terminationCondition = () -> true;
									stopQ.offer("");
								} finally {
									controllerThread.interrupt();
								}
							}
						});
					}
				}

			} catch (InterruptedException e) {
				listeners.forEach(l -> l.interrupted());
			}
		}

		// unblock someone waiting for completion
		stopQ.offer("");
		threadPool.shutdown();
		controllerThread = null;
		Thread.currentThread().interrupt();
		listeners.forEach(l -> l.terminating(nbPastEvents));

	}

	private Event<PointInTime> sleepsUntilNextEvent() {
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

	

	public double now() {
		if (!isStarted())
			return 0;

		return (Date.time() - startTime) * timeAccelarationFactor;
	}

	public void schedule(Event<PointInTime> newEvent) {
		Cout.debug("schedule " + newEvent);

		eventQueue.offer(newEvent);
		listeners.forEach(l -> l.eventSubmitted(newEvent));

		if (controllerThread != null) {
			controllerThread.interrupt();
		}
	}

	public void setTerminationCondition(Supplier<Boolean> c) {
		this.terminationCondition = c;
		
		if (controllerThread != null) {
			controllerThread.interrupt();
		}
	}

	
	public void scheduleAt(double date, String descr, Runnable r) {
		schedule(new Event<PointInTime>(descr, new PointInTime(date)) {

			@Override
			public void run() {
				r.run();
			}
		});
	}

	public void scheduleNow(Runnable r) {
		schedule(new Event<PointInTime>(null, new PointInTime(now())) {

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
	
	public void stop() throws Throwable {
		if (!isStarted())
			throw new IllegalStateException();

		var a = controllerThread;
		controllerThread = null;
		a.interrupt();
	}

	public void scheduleTerminationAt(double date, Runnable terminationCode) {
		Idawi.agenda.scheduleAt(date, "termination", () -> {
			Idawi.agenda.terminationCondition = () -> true;
			Cout.debugSuperVisible(Idawi.agenda.now() + " THIS IS THE END");
			terminationCode.run();
		});
	}
}
