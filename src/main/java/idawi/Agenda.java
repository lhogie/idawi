package idawi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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

	public List<AgendaListener> listeners = new ArrayList<>();

	private List<Event<?>> scheduledEventsQueue = new ArrayList<>();
	private long nbPastEvents = 0;
	private double startTime = -1;
	private double timeAccelarationFactor = 1;
	private Thread controllerThread;
	private Supplier<Boolean> terminationCondition;
	private BlockingQueue stopQ = new ArrayBlockingQueue(1);

	public synchronized void start() {
		if (isStarted()) {
			//throw new IllegalStateException("already started");
		} else {
			startTime = Date.time();

			threadPool.submit(() -> {
				try {
					processEventQueue();
				} catch (Throwable err) {
					err.printStackTrace();
				}
			});

			// startPeriodicDummyActivity(1);
		}
	}

	private void startPeriodicDummyActivity(double period) {
		class OKEvent extends Event<PointInTime> {

			OKEvent(double when) {
				super(new PointInTime(when));
				name = "just acticity";
			}

			@Override
			public void run() {
				schedule(new OKEvent(Idawi.agenda.time() + period));
			}
		}

		schedule(new OKEvent(1));
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
					double wait = candidateEvent.when.time - time();

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

	public double time() {
		if (!isStarted())
			return 0;

		return (Date.time() - startTime) * timeAccelarationFactor;
	}

	public void schedule(Event<PointInTime> newEvent) {
//		Cout.debug("schedule " + newEvent);

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
		schedule(new Event<PointInTime>(null, new PointInTime(time())) {

			@Override
			public void run() {
				r.run();
			}
		});
	}

	public long stopNow(Runnable terminationCode) throws Throwable {
		return stopWhen(() -> true, terminationCode);
	}

	public long stopWhen(Supplier<Boolean> terminationCondition, Runnable terminationCode) throws Throwable {
		setTerminationCondition(terminationCondition);

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
		} finally {
			if (terminationCode != null) {
				terminationCode.run();
			}
		}
	}

	private void stop() throws Throwable {
		if (!isStarted())
			throw new IllegalStateException();

		var a = controllerThread;
		controllerThread = null;
		a.interrupt();
	}
}
