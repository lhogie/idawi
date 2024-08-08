package idawi.transport.serial;

public interface Callback {
	byte[] marker();

	void callback(byte[] buf, SerialDriver d);
}