package idawi.messaging;

import toools.SizeOf;

public class ProgressMessage extends ProgressInformation {
	final public String msg;

	public ProgressMessage(String msg) {
		this.msg = msg;
	}

	@Override
	public String toString() {
		return msg;
	}
	

	@Override
	public long sizeOf() {
		return SizeOf.sizeOf(msg);
	}
}