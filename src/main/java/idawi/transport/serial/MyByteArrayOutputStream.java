package idawi.transport.serial;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class MyByteArrayOutputStream extends ByteArrayOutputStream {

	public boolean endsBy(byte[] marker) throws UnsupportedEncodingException {
		if (count < marker.length)
			return false;

		return Arrays.equals(buf, count - marker.length, count, marker, 0, marker.length);
	}
}
