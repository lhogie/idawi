package idawi.service.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jaseto.Jaseto;
import toools.exceptions.NotYetImplementedException;
import toools.io.ser.Serializer;

public class JasetoSerializer<E> extends Serializer<E> {
	Jaseto j = new Jaseto();

	@Override
	public E read(InputStream is) throws IOException {
		throw new NotYetImplementedException();
	}

	@Override
	public void write(E o, OutputStream os) throws IOException {
		os.write(j.toJSON(o).getBytes());
	}

	@Override
	public String getMIMEType() {
		return "jaseto";
	}

}
