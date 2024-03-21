package idawi.service.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jaseto.Jaseto;
import toools.exceptions.NotYetImplementedException;
import toools.io.ser.Serializer;

public class JasetoSerializer<E> extends Serializer<E> {
	Jaseto j = new Jaseto();

	public JasetoSerializer(Jaseto j) {
		this.j = j;
	}

	@Override
	public E read(InputStream is) throws IOException {
		throw new NotYetImplementedException();
	}

	@Override
	public void write(E o, OutputStream os) throws IOException {
		j.registry.clear();
		os.write(j.toNode(o).toJSON().getBytes());
	}

	@Override
	public String getMIMEType() {
		return "jaseto";
	}

	@Override
	public boolean isBinary() {
		return false;
	}

}
