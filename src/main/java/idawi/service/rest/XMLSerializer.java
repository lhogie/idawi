package idawi.service.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import toools.io.ser.Serializer;

public class XMLSerializer<E> extends Serializer<E> {
	@Override
	public E read(InputStream is) throws IOException {
		XmlMapper xmlMapper = new XmlMapper();
		throw new IllegalStateException();
	}

	@Override
	public void write(E o, OutputStream out) throws IOException {
		XmlMapper xmlMapper = new XmlMapper();
		xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
		xmlMapper.writeValue(out, o);
	}

	@Override
	public String getMIMEType() {
		return "XML";
	}
}