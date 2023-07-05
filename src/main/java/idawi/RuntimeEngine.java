package idawi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import idawi.service.local_view.LocalViewService;
import idawi.transport.Topologies;
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

	static ArrayBlockingQueue stopQ = new ArrayBlockingQueue(1);

	public static List<RuntimeListener> listeners = new ArrayList<>();

	static List<Event<?>> runningEvents = new ArrayList<>();
	static long nbPastEvents = 0;
	public static final double startTime = Date.time();
	public static double timeAcceleartionFactor = 1;
	private static Thread controllerThread;

	public static void simulationMode() {
//		simulatedTime = new AtomicDouble(0);
	}

	static Event<PointInTime> pendingEvent;
public static AtomicBoolean terminationRequired = new AtomicBoolean(false); 
	static {
		threadPool.submit(() -> {
			controllerThread = Thread.currentThread();

			while (true) {
				// if we do a simulation and nothing can generate new events, so take() has not
				// chance to return
				if (terminationRequired.get()) {
					listeners.forEach(l -> l.terminating(nbPastEvents));
					stopQ.offer("");
					break;
				} else {
					try {
						while (true) {
							listeners.forEach(l -> l.waitingForEvent());
							pendingEvent = eventQueue.take();
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
						threadPool.submit(new Runnable() {
							Event<?> e = pendingEvent;
							{
								runningEvents.add(e);
							}

							public void run() {
								listeners.forEach(l -> l.eventProcessingStarts(e));
								e.run();
								++nbPastEvents;
								runningEvents.remove(e);
								listeners.forEach(l -> l.eventProcessingCompleted(e));
							}
						});
						pendingEvent = null;
					} catch (InterruptedException err) {
						err.printStackTrace();
					}
				}
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
		public void newEventTakenFromQueue(Event<?> e) {
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
		pdf.setContent(Topologies
				.toDot(s.components(), s.links(), c -> c.toString().replaceFirst(s.component + "/", "")).toPDF());
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

	public static long blockUntilSimulationHasCompleted() {
		try {
			stopQ.take();
			stopPlatformThreads();
			return nbPastEvents;
		} catch (InterruptedException e) {
			throw new IllegalStateException();
		}
	}

	public static void terminateAt(int i) {
		offer(new TerminationEvent(20));		
	}
}
