package idawi.service;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import idawi.Component;
import idawi.Service;
import idawi.service.publish_subscribe.PublishSubscribe;
import toools.extern.Proces;

public class SystemMonitor extends Service {
	public static class Uptime implements Serializable {
		Uptime(String s) {
			this.stdout = s;
		}

		public String stdout;
	}

	public static class cpuinfo implements Serializable {
		cpuinfo(String s) {
			this.stdout = s;
		}

		public String stdout;
	}

	@Override
	public String getFriendlyName() {
		return "system monitor";
	}

	public static class Info implements Serializable {
		public double loadAvg;
		public int nbCores;
		public Uptime uptime;
		public cpuinfo cpuinfo;
		public Properties systemProperties;// = System.getProperties();

		@Override
		public String toString() {
			return "Info [loadAvg=" + loadAvg + ", nbCores=" + nbCores
					+ ", systemProperties=" + systemProperties + "]";
		}
	}

	public SystemMonitor(Component peer) {
		super(peer);
		PublishSubscribe ps = component.lookupService(PublishSubscribe.class);

		newThread_loop_periodic(20000, () -> {
			Info i = new Info();
			i.loadAvg = ManagementFactory.getOperatingSystemMXBean()
					.getSystemLoadAverage();
			i.nbCores = Runtime.getRuntime().availableProcessors();
			i.systemProperties = System.getProperties();

			if (Proces.commandIsAvailable("uptime")) {
				i.uptime = new Uptime(new String(Proces.exec("uptime")));
			}

			ps.publish(i, "system monitor");
		});

		System.out.println("*** starting service " + getClass());
	}

	@Override
	public void shutdown() {
	}
}
