package idawi.service;

import java.io.Serializable;
import java.util.Properties;

import idawi.Component;
import idawi.TypedOperation;
import idawi.Service;
import idawi.Utils;
import idawi.service.publish_subscribe.PublishSubscribe;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public class SystemMonitor extends Service {
	private Info lastInfo;

	@Override
	public String getFriendlyName() {
		return "system monitor";
	}

	public static class Info implements Serializable {
		public DoubleList loadAvg = new DoubleArrayList();
		public int nbCores;
		public Properties systemProperties;// = System.getProperties();

		@Override
		public String toString() {
			return "Info [loadAvg=" + loadAvg + ", nbCores=" + nbCores + ", systemProperties=" + systemProperties + "]";
		}
	}

	public SystemMonitor(Component peer) {
		super(peer);
		registerOperation(new get());

		newThread_loop_periodic(20000, () -> {
			Info i = new Info();
			i.loadAvg.add(Utils.loadRatio());

			if (i.loadAvg.size() > 100) {
				i.loadAvg.removeElements(0, 1);
			}

			i.nbCores = Runtime.getRuntime().availableProcessors();
			i.systemProperties = System.getProperties();
			this.lastInfo = i;

			var ps = component.lookup(PublishSubscribe.class);

			if (ps != null) {
				ps.publish(i, "system monitor");
			}
		});
	}

	public class get extends TypedOperation {

		@Override
		public String getDescription() {
			return "gets the lastest proble";
		}

		public Info f() {
			return lastInfo;
		}
	}
}
