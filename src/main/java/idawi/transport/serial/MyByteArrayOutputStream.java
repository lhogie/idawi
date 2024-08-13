package idawi.transport.serial;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyByteArrayOutputStream extends ByteArrayOutputStream {

	public boolean endsBy(byte[] marker) throws UnsupportedEncodingException {

		if (count < marker.length)
			return false;
		boolean equals = Arrays.equals(buf, count - marker.length, count, marker, 0, marker.length);

		return equals;
	}

	public boolean endsByData() throws UnsupportedEncodingException {
		Pattern pattern = Pattern.compile("S15:.*[\r\n]+");
		Matcher matcher = pattern.matcher(new String(buf));
		if (matcher.find()) {
			return true;
		}
		return false;
	}
}
