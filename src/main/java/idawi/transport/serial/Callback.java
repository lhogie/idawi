package idawi.transport.serial;

public interface Callback {
	byte[] marker();

	void impl(byte[] buf, SerialDriver d);
}