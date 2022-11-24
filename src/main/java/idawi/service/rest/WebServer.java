package idawi.service.rest;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiPredicate;

import javax.crypto.SecretKey;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.InnerOperation;
import idawi.Message;
import idawi.MessageCollector;
import idawi.OperationAddress;
import idawi.OperationParameterList;
import idawi.RegistryService;
import idawi.RemotelyRunningOperation;
import idawi.Service;
import idawi.To;
import idawi.TypedInnerOperation;
import toools.io.JavaResource;
import toools.io.Utilities;
import toools.io.file.RegularFile;
import toools.io.ser.FSTSerializer;
import toools.io.ser.GSONSerializer;
import toools.io.ser.JSONExSerializer;
import toools.io.ser.JacksonJSONSerializer;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;
import toools.io.ser.TOMLSerializer;
import toools.io.ser.ToBytesSerializer;
import toools.io.ser.ToStringSerializer;
import toools.io.ser.XMLSerializer;
import toools.io.ser.YAMLSerializer;
import toools.net.NetUtilities;
import toools.reflect.Clazz;
import toools.text.TextUtilities;

public class WebServer extends Service {
	public static int DEFAULT_PORT = 8081;
	public static final Map<String, BiPredicate<OperationAddress, MessageCollector>> stoppers = new HashMap<>();
	public static Map<String, Serializer> name2serializer = new HashMap<>();
	public static final Map<String, Class<? extends Service>> friendyName_service = new HashMap<>();
	public static Map<String, WhatToSend> whatToSends = new HashMap<>();

	static interface WhatToSend {
		Object f(Message msg);
	}

	static {
		whatToSends.put("msg", msg -> msg);
		whatToSends.put("content", msg -> msg.content);
		whatToSends.put("route", msg -> msg.route.components());
		whatToSends.put("src", msg -> msg.route.source().component.name);

		stoppers.put("aeot", (to, c) -> c.messages.filter(m -> m.isEOT()).senders().equals(to.sa.to.componentNames));
		stoppers.put("1eot", (to, c) -> !c.messages.filter(m -> m.isEOT()).isEmpty());
		stoppers.put("1m", (to, c) -> !c.messages.isEmpty());
		stoppers.put("1r", (to, c) -> !c.messages.filter(m -> m.isResult()).isEmpty());

		name2serializer.put("gson", new GSONSerializer<>());
		name2serializer.put("json_jackson", new JacksonJSONSerializer<>());
		name2serializer.put("jsonex", new JSONExSerializer<>());
		name2serializer.put("ser", new JavaSerializer<>());
		name2serializer.put("fst", new FSTSerializer<>());
		name2serializer.put("xml", new XMLSerializer<>());
		name2serializer.put("toString", new ToStringSerializer<>());
		name2serializer.put("error", new StrackTraceSerializer<>());
		name2serializer.put("bytes", new ToBytesSerializer<>());
		name2serializer.put("toml", new TOMLSerializer<>());
		name2serializer.put("yaml", new YAMLSerializer<>());
		name2serializer.put("jaseto", new IdawiWebSerializer());
	}

	private HttpServer httpServer;
	private int port;

	public WebServer(Component t) {
		super(t);
	}

	public class Base64URL extends TypedInnerOperation {
		class A {
			String routing;
			String service;
			String operation;
		}

		public void f(String url) throws IOException {
			var text = new String(Base64.getMimeDecoder().decode(url));
			var p = new Properties();
			p.load(new StringReader(text));
		}

		@Override
		public String getDescription() {
			return "interpret URLs encoded in base64";
		}

	}

	@Override
	public void dispose() {
		super.dispose();
		httpServer.stop(0);
	}

	public int getPort() {
		return port;
	}

	public HttpServer startHTTPServer() throws IOException {
		return startHTTPServer(DEFAULT_PORT);
	}

	public HttpServer startHTTPServer(int port) throws IOException {
		if (httpServer != null) {
			throw new IOException("REST server is already running");
		}

		httpServer = HttpServer.create(new InetSocketAddress(port), 0);
		httpServer.createContext("/", e -> {
			URI uri = e.getRequestURI();
			InputStream input = "POST".equals(e.getRequestMethod()) ? e.getRequestBody() : null;
			// is.close();
			// Cout.debugSuperVisible(data.length);
			List<String> path = path(uri.getPath());
			Map<String, String> query = query(uri.getQuery());
			e.getResponseHeaders().add("Access-Control-Allow-Origin:", "*	");
			OutputStream output = e.getResponseBody();

			try {
				if (path == null) {
					writeOneShot(HttpURLConnection.HTTP_OK, "text/html",
							new JavaResource(getClass(), "root.html").getByteArray(), e, output);
//					output.write(new JavaResource(getClass(), "root.html").getByteArray());
				} else {
//					Cout.debugSuperVisible("path: " + path);
					String context = path.remove(0);

					if (context.equals("api")) {
						// setting default serializer
						Serializer serializer = name2serializer.get("jaseto");

						try {
							var preferredOuputFormat = removeOrDefault(query, "format", serializer.getMIMEType(),
									name2serializer.keySet());
							serializer = name2serializer.get(preferredOuputFormat);

							e.getResponseHeaders().set("Content-type", "text/event-stream");
							e.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
							serveAPI(path, query, input, output, serializer);
						} catch (Throwable err) {
							sendEvent(output, new ChunkHeader("error", List.of(serializer.getMIMEType())),
									serializer.toBytes(err), serializer.isBinary());
						} finally {
							sendEvent(output, new ChunkHeader("EOT", List.of(serializer.getMIMEType())),
									"ok".getBytes(), false);
						}
					} else if (context.equals("favicon.ico")) {
						writeOneShot(HttpURLConnection.HTTP_OK, "image/x-icon",
								new JavaResource(WebServer.class, "flavicon.ico").getByteArray(), e, output);
					} else if (context.equals("frontend")) {
						if (path.isEmpty()) {
							path.add(new JavaResource(getClass(), "web/index.html").getPath());
						}

						writeOneShot(HttpURLConnection.HTTP_OK, guessMIMEType(path),
								new JavaResource("/" + TextUtilities.concatene(path, "/")).getByteArray(), e, output);
					} else if (context.equals("forward")) {
						var filename = TextUtilities.concatene(path, "/");
						byte[] bytes = null;
						String src = path.remove(0);

						if (src.equals("files")) { // enables the dev of frontend code
							bytes = new RegularFile("$HOME/public_html/idawi/" + filename).getContent();
						} else if (src.equals("web")) {
							bytes = NetUtilities.retrieveURLContent(filename);
						} else {
							throw new IllegalArgumentException("unknown forward source: " + src);
						}

						writeOneShot(HttpURLConnection.HTTP_OK, guessMIMEType(path), bytes, e, output);
					} else {
						throw new IllegalArgumentException("unknown context: " + context);
					}
				}
			} catch (Throwable err) {
				try {
					System.err.println("The following error will be sent to the Web client");
					err.printStackTrace();
					writeOneShot(HttpURLConnection.HTTP_INTERNAL_ERROR, "text/plain",
							TextUtilities.exception2string(err).getBytes(), e, output);
					logError(err.getMessage());
				} catch (Throwable ee) {
					ee.printStackTrace();
				}
			}

			output.close();
		});

		httpServer.setExecutor(Service.threadPool);
		httpServer.start();
		this.port = port;
		return httpServer;
	}

	private String guessMIMEType(List<String> path) {
		if (path == null || path.isEmpty()) {
			return "application/octet-stream";
		}
		var p = path.get(path.size() - 1);
		try {
			var r = Files.probeContentType(Path.of(p));
			return r == null ? "application/octet-stream" : r;
		} catch (IOException e) {
			return "application/octet-stream";
		}
		/*
		 * int i = p.lastIndexOf('.');
		 * 
		 * if (i == -1) { return "application/octet-stream"; }else { switch
		 * (p.substring(i + 1)) { case "html": return "text/html"; case "js": return
		 * "text/javascript"; case "txt": return "text/plain"; default: return
		 * "application/octet-stream"; } }
		 */
	}

	public static void writeOneShot(int returnCode, String mimeType, byte[] bytes, HttpExchange e, OutputStream os)
			throws IOException {
		e.getResponseHeaders().set("Content-type", mimeType);
		e.sendResponseHeaders(returnCode, bytes.length);
		os.write(bytes);
	}

	private static void sendEvent(OutputStream out, ChunkHeader header, byte[] data, boolean base64) {
		try {
			out.write("data: ".getBytes());
			out.write(header.toJSON().getBytes());
			out.write('\n');

			if (base64) {
				data = base64(data).getBytes();
			}

			var dataText = TextUtilities.prefixEachLineBy(new String(data), "data: ");
			out.write(dataText.getBytes());
			out.write('\n');
			out.write('\n');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private static String base64(byte[] bytes) {
		return new String(Base64.getMimeEncoder().encode(bytes)).replace("\n", "").replace("\r", "");
	}

	private static byte[] unbase64(byte[] bytes) {
		return Base64.getMimeDecoder().decode(bytes);
	}

	private void serveAPI(List<String> path, Map<String, String> query, InputStream postDataInputStream,
			OutputStream output, Serializer serializer) throws IOException {
		double duration = Double.valueOf(removeOrDefault(query, "duration", "1", null));
		double timeout = Double.valueOf(removeOrDefault(query, "timeout", "" + duration, null));
		boolean compress = Boolean.valueOf(removeOrDefault(query, "compress", "no", Set.of("yes", "no")));
		boolean encrypt = Boolean.valueOf(removeOrDefault(query, "compress", "no", Set.of("yes", "no")));
		String whatToSend = removeOrDefault(query, "whatToSend", "msg", whatToSends.keySet());
		var stop = stoppers.get(removeOrDefault(query, "stop", "aeot", stoppers.keySet()));

		if (!query.isEmpty()) {
			throw new IllegalStateException("unused parameters: " + query.keySet().toString());
		}

		// no target component are specified: let's consider the local one
		if (path.isEmpty()) {
			path.add("");
		}

		Set<ComponentDescriptor> targets = componentsFromURL(path.remove(0));

		// there's no service specified
		if (path.isEmpty()) {
			path.add(RegistryService.class.getName());
			path.add(InnerOperation.name(RegistryService.local.class));
		} // there no operation specified, let's describe the service
		else if (path.size() == 1) {
			path.add(InnerOperation.name(Service.DescriptorOperation.class));
		}

		String serviceName = path.remove(0);
		Class<? extends Service> serviceID = Clazz.findClass(serviceName);

		if (serviceID == null)
			throw new IllegalArgumentException("no such class " + serviceName);

		String operation = path.remove(0);
		var parms = new OperationParameterList();
		parms.addAll(path);

		System.out.println("calling operation " + targets + "/" + serviceID.toString() + "/" + operation
				+ " with parameters: " + parms);
		var to = new To(targets).s(serviceID).o(operation);

		RemotelyRunningOperation ro = exec(to, true, parms);
		var aes = new AESEncrypter();
		SecretKey key = null;

		if (postDataInputStream != null) {

			new Thread(() -> {
				var r = new BufferedReader(new java.io.InputStreamReader(postDataInputStream));
				ChunkHeader header = null;
				ByteArrayOutputStream content = null;

				while (true) {
					String l = null;

					try {
						l = r.readLine();
					} catch (IOException e) {
						// l will remain null
					}

					if (l == null) { // EOF
						send(ro, header, content, aes, key);
						break;
					} else if (l.isEmpty()) { // end of event
						send(ro, header, content, aes, key);
						header = null;
						content = null;
					} else if (header == null) { // new event
						header = new Gson().fromJson(l, ChunkHeader.class);
						content = new ByteArrayOutputStream();
					} else { // some content for the current event
						try {
							content.write(l.getBytes());
						} catch (IOException e) {
							throw new IllegalStateException(e);
						}
					}
				}

				try {
					r.close();
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}).start();
		}

		ro.returnQ.recv_sync(duration, timeout, c -> {
			List<String> encodings = new ArrayList<>();
			var bytes = serializer.toBytes(whatToSends.get(whatToSend).f(c.messages.last()));
			encodings.add(serializer.getMIMEType());
			boolean base64 = serializer.isBinary();

			if (compress) {
				bytes = Utilities.gzip(bytes);
				encodings.add("gzip");
				base64 = true;
			}

			if (encrypt) {
				try {
					bytes = aes.encrypt(bytes, key);
					encodings.add("aes");
					base64 = true;
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}

			sendEvent(output, new ChunkHeader("component message", encodings), bytes, base64);

//			c.timeout = ?

			c.stop = stop.test(to, c);
		});

//			ro.dispose();

	}

	private void send(RemotelyRunningOperation ro, ChunkHeader header, ByteArrayOutputStream content, AESEncrypter aes,
			SecretKey key) {
		var bytes = content.toByteArray();

		// restore the original data by reversing the encoding
		for (int i = header.syntax.size() - 1; i > 0; --i) {
			var deser = header.syntax.get(i);

			if (deser.equals("gzip")) {
				bytes = Utilities.gunzip(bytes);
			} else if (deser.equals("base64")) {
				bytes = unbase64(bytes);
			} else if (deser.equals("aes")) {
				try {
					bytes = aes.decrypt(bytes, key);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
		}

		String serializerName = header.syntax.get(0);
		var ser = name2serializer.get(serializerName);
		Object o = ser.fromBytes(bytes);
	}

	private void send(RemotelyRunningOperation ro, ChunkHeader header, ByteArrayOutputStream content) {
		// TODO Auto-generated method stub

	}

	private void send(RemotelyRunningOperation ro, ByteArrayOutputStream content) {
		// TODO Auto-generated method stub

	}

	private List<String> path(String s) {
		if (s == null) {
			return null;
		}

		if (s.startsWith("/")) {
			s = s.substring(1);
		}

		if (s.endsWith("/")) {
			s = s.substring(0, s.length() - 1);
		}

		return s.isEmpty() ? null : new ArrayList<>(Arrays.asList(s.split("/")));
	}

	private static String removeOrDefault(Map<String, String> map, String k, String defaultValue,
			Set<String> validKeys) {
		var r = map.remove(k);

		if (r == null)
			r = defaultValue;

		if (validKeys != null && !validKeys.contains(r))
			throw new IllegalArgumentException(
					r + " is not a valid value for '" + k + "'. Valid values are: " + validKeys);

		return r;
	}

	private Set<ComponentDescriptor> componentsFromURL(String s) {
		if (s.isEmpty()) {
			return null;
		}

		Set<ComponentDescriptor> components = new HashSet<>();

		for (String name : s.split(",")) {
			var found = component.operation(RegistryService.lookUp.class).f(name);

			if (found == null) {
				components.add(ComponentDescriptor.fromCDL("name=" + name));
			} else {
				components.add(found);
			}
		}

		return components;
	}

	private static Map<String, String> query(String s) {
		Map<String, String> query = new HashMap<>();

		if (s != null && !s.isEmpty()) {
			for (String queryEntry : s.split("&")) {
				String[] a = queryEntry.split("=");

				if (a.length == 2) {
					query.put(a[0], a[1]);
				} else {
					query.put(a[0], null);
				}
			}
		}

		return query;
	}

	public class stopHTTPServer extends TypedInnerOperation {
		public void f() throws IOException {
			if (httpServer == null) {
				throw new IOException("REST server is not running");
			}

			httpServer.stop(0);
			httpServer = null;
		}

		@Override
		public String getDescription() {
			return "stops the HTTP server";
		}
	}

	public class startHTTPServerOperation extends TypedInnerOperation {
		public void f(int port) throws IOException {
			startHTTPServer(port);
		}

		@Override
		public String getDescription() {
			return "starts the HTTP server";
		}
	}

}
