package idawi.transport.serial;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class MyByteArrayOutputStream extends ByteArrayOutputStream {

	public boolean endsBy(byte[] marker) throws UnsupportedEncodingException {
		System.out.println("yaa");
		System.out.println(marker.length);
		return Arrays.equals(buf, count - marker.length, count, marker, 0, marker.length);
	}
}
