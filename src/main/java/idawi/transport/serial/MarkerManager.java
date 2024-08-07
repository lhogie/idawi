package idawi.transport.serial;

public interface MarkerManager {
	byte[] marker();

	void callBack(byte[] buf, SerialDriver d);
}