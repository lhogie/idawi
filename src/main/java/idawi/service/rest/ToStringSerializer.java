package idawi.service.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import toools.io.ser.Serializer;
import toools.text.TextUtilities;

public class ToStringSerializer<E> extends Serializer<E> {
	@Override
	public E read(InputStream is) throws IOException {
		throw new IllegalStateException();
	}

	@Override
	public void write(E o, OutputStream out) throws IOException {
		if (o instanceof Throwable) {
			((Throwable) o).printStackTrace(new PrintStream(out));
		} else {
			out.write(TextUtilities.toString(o).getBytes());
		}
	}

	@Override
	public String getMIMEType() {
		return "toString()";
	}
}