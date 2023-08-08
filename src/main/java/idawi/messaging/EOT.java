package idawi.messaging;

import java.io.Serializable;

import toools.SizeOf;

public class EOT implements Serializable, SizeOf {
	private EOT() {

	}

	@Override
	public String toString() {
		return "EOT";
	}

	public static final EOT instance = new EOT();

	@Override
	public long sizeOf() {
		return 1;
	}
}