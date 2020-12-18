package idawi.service.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import toools.io.ser.Serializer;

public class StrackTraceSerializer<E> extends Serializer<E> {
	@Override
	public E read(InputStream is) throws IOException {
		throw new IllegalStateException();
	}

	@Override
	public void write(E o, OutputStream out) throws IOException {
		((Throwable) o).printStackTrace(new PrintStream(out));
	}

	@Override
	public String getMIMEType() {
		return "stack_trace";
	}
}