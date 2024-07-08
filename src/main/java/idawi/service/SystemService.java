package idawi.service;

import java.io.IOException;
import java.util.Properties;

import idawi.Binaries;
import idawi.Component;
import idawi.FunctionEndPoint;
import idawi.Service;
import idawi.SupplierEndPoint;
import idawi.service.local_view.ComponentInfo;
import idawi.service.time.TimeService;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class SystemService extends Service {

	public int nbCores;
	private ComponentInfo localInfo;

	public SystemService(Component node) {
		super(node);
		this.nbCores = Runtime.getRuntime().availableProcessors();
	}

	public ComponentInfo localComponentInfo() {
		if (localInfo == null || localInfo.reliability(component.now()) < 0.1) {
			localInfo = new ComponentInfo(component.now());
			localInfo.component = component;
			localInfo.location = component.getLocation();
			localInfo.timeModel = component.service(TimeService.class).model;
			localInfo.systemInfo = component.service(SystemMonitor.class).info.get();
			component.forEachService(s -> localInfo.services.add(s.descriptor()));
		}

		return localInfo;
	}

	public class info extends SupplierEndPoint<ComponentInfo> {

		@Override
		public ComponentInfo get() {
			return localComponentInfo();
		}

		@Override
		protected String r() {
			return "an updated description of the host component";
		}
	}

	public class binaryFetchBytes extends FunctionEndPoint<long[], byte[]> {

		@Override
		public String getDescription() {
			return "extract bytes from the binary files";
		}

		@Override
		public byte[] f(long[] indices) {
			return Binaries.proofOfBinaries(indices);
		}
	}

	public class binaryHash extends SupplierEndPoint<Long> {

		@Override
		public Long get() throws IOException {
			return Binaries.hashBinaries();
		}

		@Override
		protected String r() {
			return "return a hash of the binaries";
		}
	}

	public class binarySize extends SupplierEndPoint<Long> {

		public Long get() {
			return Binaries.binarySize();
		}

		@Override
		protected String r() {
			return "the size of the binaries";
		}

	}

	public class systemProperties extends SupplierEndPoint<Properties> {

		@Override
		public Properties get() {
			return System.getProperties();
		}

		@Override
		protected String r() {
			return "gets the system properties";
		}
	}
}
