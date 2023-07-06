package idawi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;
import java.util.function.Supplier;

import idawi.service.local_view.LocalViewService;
import toools.io.file.Directory;
import toools.io.file.RegularFile;
import toools.net.SSHParms;
import toools.reflect.ClassPath;
import toools.util.Date;

public class RuntimeEngine {
	public static ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
//	public static AtomicDouble simulatedTime;
	private static PriorityBlockingQueue<Event<PointInTime>> eventQueue = new PriorityBlockingQueue<>(100,
			(a, b) -> a.when.compareTo(b.when));

	private static ArrayBlockingQueue stopQ = new ArrayBlockingQueue(1);

	public final static List<RuntimeListener> listeners = new ArrayList<>();

	private static List<Event<?>> runningEvents = new ArrayList<>();
	static long nbPastEvents = 0;
	public static final double startTime = Date.time();
	public static double timeAcceleartionFactor = 1;
	private static Thread controllerThread;
	private static Event<PointInTime> pendingEvent;
	public static Supplier<Boolean> terminationCondition = () -> false;

	static {
		threadPool.submit(() -> {
			try {
				controllerThread = Thread.currentThread();

				while (true) {
					// if we do a simulation and nothing can generate new events, so take() has not
					// chance to return
					if (terminationCondition.get()) {
						listeners.forEach(l -> l.terminating(nbPastEvents));
						stopQ.offer("");
						break;
					} else {
						while (true) {
							listeners.forEach(l -> l.waitingForEvent());
							while (true) {
								try {
									pendingEvent = eventQueue.take();
									break;
								} catch (InterruptedException interupt) {
									System.err.println("weird");
								}
							}
							listeners.forEach(l -> l.newEventTakenFromQueue(pendingEvent));
							double waitTimeS = Math.max(0, pendingEvent.when.time - now());
							long waitTimeMs = (long) (1000 * waitTimeS);

							try {
								listeners.forEach(l -> l.sleeping(waitTimeMs));
								Thread.sleep(waitTimeMs);
								break;
							} catch (InterruptedException interupt) {
								listeners.forEach(l -> l.interrupted());
								eventQueue.offer(pendingEvent);
							}
						}

//						simulatedTime.set(e.when.time);
						listeners.forEach(l -> l.newEventScheduledForExecution(pendingEvent));

						// if (!threadPool.isShutdown())
						threadPool.submit(new Runnable() {
							Event<?> e = pendingEvent;
							{
								runningEvents.add(e);
							}

							public void run() {
								try {
									listeners.forEach(l -> l.eventProcessingStarts(e));
									e.run();
									++nbPastEvents;
									runningEvents.remove(e);
									listeners.forEach(l -> l.eventProcessingCompleted(e));
								} catch (Throwable err) {
									stopQ.offer(err);
									terminationCondition = () -> true;
								}
							}
						});
						pendingEvent = null;
					}
				}
			} catch (Throwable err) {
				err.printStackTrace();
			}
		});
	}

	public static double now() {
//		return simulatedTime == null ? Date.time() : simulatedTime.get();
		return (Date.time() - startTime) * timeAcceleartionFactor;
		// var ts = lookup(TimeService.class);
		// return ts == null ? Date.time() : ts.now();
	}

	public static void stopPlatformThreads() {
		threadPool.shutdown();
	}

	private static abstract class LocalViewPlotter extends RuntimeAdapter {
		private Directory dir;
		private LocalViewService localView;

		public LocalViewPlotter(Directory dir, LocalViewService localView) {
			this.dir = dir;
			this.localView = localView;
		}

		@Override
		public void eventProcessingCompleted(Event<?> e) {
			if (plot(e)) {
				doImg(localView, now(), dir, e.getClass().getSimpleName());
			}
		}

		public abstract boolean plot(Event<?> e);
	}

	public static RegularFile plotNet(LocalViewService localView, Directory dir, Predicate<Event<?>> p) {
		dir.mkdirs();
		var firstImg = doImg(localView, now(), dir, "");

		RuntimeEngine.listeners.add(new LocalViewPlotter(dir, localView) {

			@Override
			public boolean plot(Event<?> e) {
				return p.test(e);
			}

		});

		return firstImg;
	}

	public static RegularFile doImg(LocalViewService s, double date, Directory dir, String label) {
		var pdf = new RegularFile(dir, "/simulated_network-" + String.format("%03d", nbPastEvents) + " date=" + date
				+ ", event=" + label + ".pdf");
		s.plot(pdf);
		return pdf;
	}

	public static void syncToI3S() throws IOException {
		var ssh = new SSHParms();
		ssh.host = "bastion.i3s.unice.fr";
		ssh.username = "hogie";
		String dir = "/net1/home/hogie/public_html/software/idawi/last_bins/";
		ClassPath.retrieveSystemClassPath().rsyncTo(ssh, dir, out -> System.out.println(out),
				err -> System.err.println(err));
		System.out.println(ssh.host + ":" + dir);
	}

	public static void offer(Event<PointInTime> newEvent) {
		listeners.forEach(l -> l.eventSubmitted(newEvent));
		eventQueue.offer(newEvent);

		if (pendingEvent != null && newEvent.when.time < pendingEvent.when.time) {
			controllerThread.interrupt();
		}
	}

	public static void offer(double date, Runnable r) {
		offer(new Event<PointInTime>(new PointInTime(date)) {

			@Override
			public void run() {
				r.run();
			}
		});
	}

	public static long blockUntilSimulationHasCompleted() throws Throwable {
		try {
			// stopPlatformThreads();
			var o = stopQ.take();

			if (o instanceof Throwable) {
				throw (Throwable) o;
			} else {
				return nbPastEvents;
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException();
		}
	}

	public static void stdout(String s) {
		System.out.println(String.format("%.3f", RuntimeEngine.now()) + "\t" + s);
	}

	public static void stderr(String s) {
		System.err.println(String.format("%.3f", RuntimeEngine.now()) + "\t" + s);
	}

}
