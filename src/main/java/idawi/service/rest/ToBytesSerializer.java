package idawi.service.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import toools.io.ser.Serializer;

public class ToBytesSerializer extends Serializer<byte[]> {

	@Override
	public byte[] read(InputStream is) throws IOException {
		return is.readAllBytes();
	}

	@Override
	public void write(byte[] o, OutputStream os) throws IOException {
		os.write(o);
	}

	@Override
	public String getMIMEType() {
		return "raw data";
	}

}
