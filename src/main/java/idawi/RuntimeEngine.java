package idawi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;

import com.google.common.util.concurrent.AtomicDouble;

import idawi.service.local_view.LocalViewService;
import idawi.transport.Topologies;
import toools.io.file.Directory;
import toools.io.file.RegularFile;
import toools.net.SSHParms;
import toools.reflect.ClassPath;
import toools.util.Date;

public class RuntimeEngine {
	public static ExecutorService threadPool = Executors.newCachedThreadPool();
	public static AtomicDouble simulatedTime;
	public static PriorityBlockingQueue<Event<SpecificTime>> eventQueue = new PriorityBlockingQueue<>(100,
			(a, b) -> a.when.compareTo(b.when));

	public static void simulationMode() {
		simulatedTime = new AtomicDouble(0);
	}

	public static List<RuntimeListener> listeners = new ArrayList<>();

	static {
		threadPool.submit(() -> {
			try {
				var e = eventQueue.take();
				simulatedTime.set(e.when.time);
				listeners.forEach(l -> l.newEvent(e));
				threadPool.submit(e);
			} catch (InterruptedException err) {
				err.printStackTrace();
			}
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

	public static abstract class LocalViewPlotter implements RuntimeListener {
		private Directory dir;
		private LocalViewService localView;

		public LocalViewPlotter(Directory dir, LocalViewService localView) {
			this.dir = dir;
			this.localView = localView;
		}

		@Override
		public void newEvent(Event<?> e) {
			if (plot(e)) {
				doImg(localView, now(), dir, e.getClass().getSimpleName());
			}
		}

		public abstract boolean plot(Event<?> e);
	}

	public static void plotNet(LocalViewService localView, Directory dir, Predicate<Event<?>> p) {
		dir.mkdirs();
		doImg(localView, now(), dir, "").open();

		RuntimeEngine.listeners.add(new LocalViewPlotter(dir, localView) {

			@Override
			public boolean plot(Event<?> e) {
				return p.test(e);
			}

			@Override
			public void eventSubmitted(Event<SpecificTime> newEvent) {
			}
		});
	}

	public static int eventIndex = 0;

	public static RegularFile doImg(LocalViewService s, double date, Directory dir, String label) {
		var pdf = new RegularFile(dir, "/simulated_network-" + String.format("%03d", eventIndex) + " date=" + date
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

	public static void offer(Event<SpecificTime> newEvent) {
		listeners.forEach(l -> l.eventSubmitted(newEvent));
		eventQueue.offer(newEvent);
	}

}
