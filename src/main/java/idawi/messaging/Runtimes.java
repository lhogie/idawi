package idawi.messaging;

import java.io.Serializable;

import toools.SizeOf;

public class Runtimes implements Serializable, SizeOf {
	public final double soonestExecTime = 0;
	public final double latestExecTime = Double.MAX_VALUE;

	@Override
	public long sizeOf() {
		return 16;
	}
}
