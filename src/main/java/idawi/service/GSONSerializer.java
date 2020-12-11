package idawi.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import toools.io.ser.Serializer;
import toools.reflect.Clazz;

public class GSONSerializer<E> extends Serializer<E> {
	public static final GSONSerializer instance = new GSONSerializer();
	public String msg;

	static class ClassAdapter extends TypeAdapter<Class> {
		@Override
		public Class read(JsonReader reader) throws IOException {
			String classname = reader.nextString();
			classname = classname.replace("-", "$");
			return Clazz.findClass(classname);
		}

		@Override
		public void write(JsonWriter writer, Class c) throws IOException {
			writer.value(c.getName().replace("$", "-"));
		}
	}

	static final Gson gson;

	static {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Class.class, new ClassAdapter());
		builder.setPrettyPrinting();
		gson = builder.create();
	}

	public static class Hodler<E> {
		E o;
		String msg;
	}

	@Override
	public E read(InputStream is) throws IOException {
		Reader r = new InputStreamReader(is);
		Hodler<E> m = gson.fromJson(r, Hodler.class);
		return m.o;
	}

	@Override
	public void write(E o, OutputStream os) throws IOException {
		Hodler<E> m = new Hodler<>();
		m.o = o;
		m.msg = msg;
		String json = gson.toJson(m);
		System.out.println("sending: " + json);
		os.write(json.getBytes());
	}

	@Override
	public String getMIMEType() {
		return "Google GSON";
	}

	public static void main(String[] args) {
		String in = "salut";
		byte[] bytes = GSONSerializer.instance.toBytes(in);
		Object out = GSONSerializer.instance.fromBytes(bytes);
		System.out.println(in.equals(out));
	}
}
