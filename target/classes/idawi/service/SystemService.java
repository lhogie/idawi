package idawi.service;

import java.io.IOException;
import java.util.Properties;

import idawi.Binaries;
import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassOperation;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class SystemService extends Service {

	public int nbCores;

	public SystemService(Component node) {
		super(node);
		registerOperation(new binaryFetchBytes());
		registerOperation(new binarySize());
		registerOperation(new systemProperties());
		this.nbCores = Runtime.getRuntime().availableProcessors();
	}

	public class binaryFetchBytes extends TypedInnerClassOperation {

		public byte[] fetch(long[] offsets) throws IOException {
			return Binaries.proofOfBinaries(offsets);
		}

		@Override
		public String getDescription() {
			return "extract bytes from the binary files";
		}
	}

	public class binaryHash extends TypedInnerClassOperation {

		public long fetch(long[] offsets) throws IOException {
			return Binaries.hashBinaries();
		}

		@Override
		public String getDescription() {
			return "return a hash of the binaries";
		}
	}

	public class binarySize extends TypedInnerClassOperation {

		public long get() throws IOException {
			return Binaries.binarySize();
		}

		@Override
		public String getDescription() {
			return "the size of the binaries";
		}
	}

	public class systemProperties extends TypedInnerClassOperation {

		public Properties get() throws IOException {
			return System.getProperties();
		}

		@Override
		public String getDescription() {
			return "gets the system properties";
		}
	}
}
