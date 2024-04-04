package idawi.service.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;

import toools.io.ser.Serializer;

public class Suggestion implements Serializable {
	final String key;
	final Object json;

	public Suggestion(String k, Object j) {
		this.key = k;
		this.json = j;
	}

	public void send(OutputStream output, Serializer serializer) throws IOException {
		WebService.sendEvent(output, new ChunkHeader(List.of(serializer.getMIMEType())), serializer.toBytes(this), false);
	}
}