package idawi.service.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import toools.io.ser.Serializer;

public class ToStringSerializer<E> extends Serializer<E> {
	@Override
	public E read(InputStream is) throws IOException {
		throw new IllegalStateException();
	}

	@Override
	public void write(E o, OutputStream out) throws IOException {
		out.write(o.toString().getBytes());
	}

	@Override
	public String getMIMEType() {
		return "toString()";
	}
}