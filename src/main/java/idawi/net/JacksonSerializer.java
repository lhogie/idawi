package idawi.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;

import toools.io.ser.Serializer;

public class JacksonSerializer<E> extends Serializer<E> {
	public static final JacksonSerializer instance = new JacksonSerializer();

	ObjectMapper objectMapper = new ObjectMapper();

	class M {
		E o;

		public E getO() {
			return o;
		}

		public void setO(E o) {
			this.o = o;
		}
	}

	@Override
	public E read(InputStream is) throws IOException {
		return (E) objectMapper.readValue(is, M.class).o;
	}

	@Override
	public void write(E o, OutputStream os) throws IOException {
		M m = new M();
		m.o = o;
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.writer().withRootName(o.getClass().getName());
		objectMapper.writeValue(os, o);
	}

	@Override
	public String getMIMEType() {
		return "Jackson JSON";
	}

	public static void main(String[] args) {
		System.out.println(new String(instance.toBytes("salut".getBytes())));
	}
}
