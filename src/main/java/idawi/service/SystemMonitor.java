package idawi.service;

import java.io.Serializable;

import fr.cnrs.i3s.Cache;
import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import toools.Systeem;

public class SystemMonitor extends Service {
	public Cache<SystemInfo> info = new Cache<>(1, () -> probe());

	@Override
	public String getFriendlyName() {
		return "system monitor";
	}

	public static class SystemInfo implements Serializable {
		public DoubleList loadAvg = new DoubleArrayList();
		public int nbCores;

		@Override
		public String toString() {
			return "loadAvg=" + loadAvg;
		}
	}

	public SystemMonitor(Component peer) {
		super(peer);
	}

	public class get extends TypedInnerClassEndpoint {

		@Override
		public String getDescription() {
			return "gets the lastest proble";
		}

		public SystemInfo f() {
			return info.get();
		}
	}

	public SystemInfo probe() {
		SystemInfo i = new SystemInfo();
		i.loadAvg.add(Systeem.loadRatio());

		if (i.loadAvg.size() > 100) {
			i.loadAvg.removeElements(0, 1);
		}

		i.nbCores = Runtime.getRuntime().availableProcessors();
		return i;
	}

	public class loadAvg extends TypedInnerClassEndpoint {

		@Override
		public String getDescription() {
			return "gives the load average";
		}

		public double f() {
			return Systeem.loadRatio();
		}
	}
}
