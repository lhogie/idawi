package idawi.service;

import java.io.IOException;
import java.util.Properties;

import idawi.Binaries;
import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
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

	public class info extends TypedInnerClassEndpoint {

		public ComponentInfo get() throws IOException {
			return localComponentInfo();
		}

		@Override
		public String getDescription() {
			return "return an updated description of the host component";
		}
	}

	public class binaryFetchBytes extends TypedInnerClassEndpoint {

		public byte[] fetch(long[] offsets) throws IOException {
			return Binaries.proofOfBinaries(offsets);
		}

		@Override
		public String getDescription() {
			return "extract bytes from the binary files";
		}
	}

	public class binaryHash extends TypedInnerClassEndpoint {

		public long fetch(long[] offsets) throws IOException {
			return Binaries.hashBinaries();
		}

		@Override
		public String getDescription() {
			return "return a hash of the binaries";
		}
	}

	public class binarySize extends TypedInnerClassEndpoint {

		public long get() throws IOException {
			return Binaries.binarySize();
		}

		@Override
		public String getDescription() {
			return "the size of the binaries";
		}
	}

	public class systemProperties extends TypedInnerClassEndpoint {

		public Properties get() throws IOException {
			return System.getProperties();
		}

		@Override
		public String getDescription() {
			return "gets the system properties";
		}
	}
}
