package idawi.service.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;

import toools.io.ser.Serializer;

public class Suggestion implements Serializable {
	final String key;
	final Object json;
	final String description;

	public Suggestion(String k, String description, Object j) {
		this.key = k;
		this.json = j;
		this.description = description;
	}

	public void send(OutputStream output, Serializer serializer) throws IOException {
		Utils.sendEvent(output, new ChunkHeader(List.of(serializer.getMIMEType())), serializer.toBytes(this),
				false);
	}
}