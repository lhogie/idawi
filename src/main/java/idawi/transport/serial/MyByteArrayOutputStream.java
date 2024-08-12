package idawi.transport.serial;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class MyByteArrayOutputStream extends ByteArrayOutputStream {

	public boolean endsBy(byte[] marker) throws UnsupportedEncodingException {

		if (count < marker.length)
			return false;
		boolean equals = Arrays.equals(buf, count - marker.length, count, marker, 0, marker.length);

		return equals;
	}

	public boolean endsByData() throws UnsupportedEncodingException {
		if (new String(buf).contains("ATI5")) {
			return true;
		}
		return false;
	}
}
