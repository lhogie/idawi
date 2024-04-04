package idawi.service.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.crypto.SecretKey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import idawi.Component;
import idawi.Endpoint;
import idawi.EndpointParameterList;
import idawi.Idawi;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.MessageCollector;
import idawi.routing.BlindBroadcasting;
import idawi.routing.ComponentMatcher;
import idawi.routing.FloodingWithSelfPruning;
import idawi.routing.RoutingParameters;
import idawi.routing.RoutingService;
import idawi.service.DemoService;
import idawi.service.ServiceManager;
import idawi.service.ServiceManager.listServices;
import idawi.service.local_view.LocalViewService;
import toools.io.Cout;
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
import toools.security.AES;
import toools.text.TextUtilities;

public class WebService extends Service {

	public static abstract class TypedObject<T> {
		T value;

		public abstract String nature();
	}

	public static abstract class HTMLRenderableObject<T> {
		protected T value;

		public abstract String html();
	}

	public static int DEFAULT_PORT = 8081;
	public static Map<String, Serializer> name2serializer = new HashMap<>();

	static {
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

	Map<String, Function<MessageCollector, Object>> whatToSendMap = new HashMap<>();
	public final Map<String, Class<? extends Service>> shortcut_service = new HashMap<>();

	public WebService(Component t) {
		super(t);

		shortcut_service.put("bb", BlindBroadcasting.class);
		shortcut_service.put("dt", LocalViewService.class);
		shortcut_service.put("fwsp", FloodingWithSelfPruning.class);
		shortcut_service.put("demo", DemoService.class);

		whatToSendMap.put("msg", c -> c.messages.last());
		whatToSendMap.put("content", c -> c.messages.last().content);
		whatToSendMap.put("route", c -> c.messages.last().route);
		whatToSendMap.put("source", c -> c.messages.last().route.source());
		whatToSendMap.put("sc", c -> new Object[] { c.messages.last().route.source(), c.messages.last().content });

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
			List<String> path = path(uri.getPath());
			Map<String, String> query = query(uri.getQuery());
			e.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			OutputStream output = e.getResponseBody();

			try {
				if (path == null) {
					singleHTTPResponse(HttpURLConnection.HTTP_OK, "text/html",
							new JavaResource(getClass(), "root.html").getByteArray(), e, output);
				} else {
					String context = path.remove(0);

					if (context.equals("api")) {
						// setting default serializer
						Serializer preferredSerializer = name2serializer.get("jaseto");

						try {
							e.getResponseHeaders().set("Content-type", "text/event-stream");
							e.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
							var preferredOuputFormat = removeOrDefault(query, "format",
									preferredSerializer.getMIMEType(), name2serializer.keySet());
							preferredSerializer = name2serializer.get(preferredOuputFormat);
							serveAPI(path, query, input, output, preferredSerializer);
							sendEvent(output, new ChunkHeader(List.of("plain")), "EOT".getBytes(), false);
						} catch (Throwable err) {
							err.printStackTrace();
							try {
								sendEvent(output, new ChunkHeader(List.of(preferredSerializer.getMIMEType())),
										preferredSerializer.toBytes(err), preferredSerializer.isBinary());
								sendEvent(output, new ChunkHeader(List.of("plain")), "EOT".getBytes(), false);
							} catch (IOException ioerr) {

							}
						}
					} else if (context.equals("favicon.ico")) {
						singleHTTPResponse(HttpURLConnection.HTTP_OK, "image/x-icon",
								new JavaResource(WebService.class, "flavicon.ico").getByteArray(), e, output);
					} else if (context.equals("frontend")) {
						if (path.isEmpty()) {
							path.add(new JavaResource(getClass(), "web/index.html").getPath());
						}

						singleHTTPResponse(HttpURLConnection.HTTP_OK, guessMIMEType(path),
								new JavaResource("/" + TextUtilities.concatene(path, "/")).getByteArray(), e, output);
					} else if (context.equals("forward")) {
						String src = path.remove(0);
						var filename = TextUtilities.concatene(path, "/");
						byte[] bytes = null;

						if (src.equals("files")) { // enables the dev of frontend code
							bytes = new RegularFile("$HOME/public_html/idawi/" + filename).getContent();
						} else if (src.equals("web")) {
//							filename = "http://" + filename;
							System.out.println("wget " + filename);
							bytes = NetUtilities.retrieveURLContent(filename);
							System.out.println(bytes.length);
						} else {
							throw new IllegalArgumentException("unknown forward source: " + src);
						}

						singleHTTPResponse(HttpURLConnection.HTTP_OK, guessMIMEType(path), bytes, e, output);
					} else {
						throw new IllegalArgumentException("unknown context: " + context);
					}
				}
			} catch (Throwable err) {
				try {
					System.err.println("The following error will be sent to the Web client");
					err.printStackTrace();
					singleHTTPResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "text/plain",
							TextUtilities.exception2string(err).getBytes(), e, output);
					logError(err.getMessage());
				} catch (IOException ee) {
//					ee.printStackTrace();
				}
			}

			output.close();
		});

		httpServer.setExecutor(Idawi.agenda.threadPool);
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
	}

	public static void singleHTTPResponse(int returnCode, String mimeType, byte[] bytes, HttpExchange e,
			OutputStream os) throws IOException {
		e.getResponseHeaders().set("Content-type", mimeType);
		e.sendResponseHeaders(returnCode, bytes.length);
		os.write(bytes);
	}

	static void sendEvent(OutputStream out, ChunkHeader header, byte[] data, boolean base64) throws IOException {
		out.write("data: ".getBytes());
		out.write(header.toJSONNode().toString().getBytes());
		out.write('\n');

		if (base64) {
			data = TextUtilities.base64(data).getBytes();
		}

		var dataText = TextUtilities.prefixEachLineBy(new String(data), "data: ");
		out.write(dataText.getBytes());
		out.write('\n');
		out.write('\n'); // end of event
		out.flush();
	}

	private void serveAPI(List<String> path, Map<String, String> query, InputStream postDataInputStream,
			OutputStream output, Serializer serializer) throws IOException, ClassNotFoundException {

		String duration = removeOrDefault(query, "stopWhen", "1s", null);
		double timeout = Double.valueOf(removeOrDefault(query, "timeout", "1", null));
		boolean compress = Boolean.valueOf(removeOrDefault(query, "compress", "false", Set.of("true", "false")));
		boolean encrypt = Boolean.valueOf(removeOrDefault(query, "encrypt", "no", Set.of("yes", "no")));
		var whatToSendF = whatToSendMap
				.get(String.valueOf(removeOrDefault(query, "what", "msg", whatToSendMap.keySet())));

		final RoutingService r = routing(query, output, serializer);
		final RoutingParameters rp = routingParms(query, r, output, serializer);
		final var matcher = matcher(query, output, r, rp, serializer);
		final var serviceClass = service(query, output, serializer, matcher, r, rp, duration);

		if (serviceClass == null)
			return;

		final var endpointClass = endpoint(query, output, serializer, matcher, r, rp, serviceClass, duration);

		if (endpointClass == null)
			return;

		final EndpointParameterList op = parmsFromQuery(query);

		if (!query.isEmpty()) {
			throw new IllegalStateException("invalid parameters: " + query.keySet().toString());
		}

		exec(r, rp, matcher, serviceClass, endpointClass, op, compress, encrypt, duration, timeout, whatToSendF,
				serializer, output, postDataInputStream);
	}

	private Class<? extends Endpoint> endpoint(Map<String, String> query, OutputStream output, Serializer serializer,
			ComponentMatcher matcher, RoutingService r, RoutingParameters rp, Class<? extends Service> s,
			String duration) throws ClassNotFoundException, IOException {
		if (query.containsKey("e")) {
			return (Class<? extends Endpoint>) Clazz.innerClass(s, query.remove("e"));
		} else {
			var ro = r.exec(matcher, s, listEndpoints.class, rp, null, true);
			var messages = ro.returnQ.collector().collectWhile(stop(duration)).messages;
			var map = new HashMap<Component, List<String>>();
			messages.forEach(m -> map.put(m.sender(),
					((Set<Class<? extends Endpoint>>) m.content).stream().map(e -> e.getSimpleName()).toList()));
			new Suggestion("e", map).send(output, serializer);
			return null;
		}
	}

	private Class<? extends Service> service(Map<String, String> query, OutputStream output, Serializer serializer,
			ComponentMatcher t, RoutingService r, RoutingParameters rp, String duration)
			throws ClassNotFoundException, IOException {
		if (query.containsKey("s")) {
			var s = query.remove("s");
			var c = shortcut_service.get(s);

			if (c != null) {
				return c;
			} else {
				return (Class<? extends Service>) Class.forName(s);
			}
		} else {
			var ro = r.exec(t, ServiceManager.class, listServices.class, rp, null, true);
			var messages = ro.returnQ.collector().collectWhile(stop(duration)).messages;
			var map = new HashMap<Component, List<? extends Service>>();
			messages.forEach(m -> map.put(m.sender(), (List<? extends Service>) m.content));
			new Suggestion("s", map).send(output, serializer);
			return null;
		}
	}

	private Predicate<MessageCollector> stop(String s) {
		if (s.matches("[0-9]s")) {
			return c -> c.duration() < Double.valueOf(s.substring(0, s.length() - 1));
		} else if (s.matches("[0-9]msg")) {
			return c -> c.messages.size() < Integer.valueOf(s.substring(0, s.length() - 3));
		}

		throw new IllegalArgumentException("cannot interpret: " + s);
	}

	private ComponentMatcher matcher(Map<String, String> query, OutputStream output, RoutingService r,
			RoutingParameters rp, Serializer serializer) throws IOException {
		if (query.containsKey("t")) {
			return ComponentMatcher.fromString(query.remove("t"), component.localView());
		} else {
			new Suggestion("t", component.localView().g.components()).send(output, serializer);
			return ComponentMatcher.all;
		}
	}

	private <P extends RoutingParameters> RoutingParameters routingParms(Map<String, String> query, RoutingService<P> r,
			OutputStream output, Serializer serializer) throws IOException {
		if (query.containsKey("rp")) {
			final RoutingParameters rp = r.defaultData();
			rp.fromString(query.remove("rp"), r);
			return rp;
		} else {
			new Suggestion("rp", r.dataSuggestions()).send(output, serializer);
			return r.defaultData();
		}
	}

	private RoutingService routing(Map<String, String> query, OutputStream output, Serializer serializer)
			throws IOException, ClassNotFoundException {
		if (query.containsKey("r")) {
			var s = query.remove("r");
			var serviceClass = shortcut_service.get(s);

			if (serviceClass == null) {
				serviceClass = (Class<? extends RoutingService>) Class.forName(s);
			}

			return (RoutingService) component.service(serviceClass);
		} else {
			new Suggestion("r", component.services(RoutingService.class).stream().map(s -> s.getClass()).toList())
					.send(output, serializer);
			return component.defaultRoutingProtocol();
		}
	}

	private EndpointParameterList parmsFromQuery(Map<String, String> query) {
		var l = new EndpointParameterList();

		for (int i = 0;; ++i) {
			if (query.containsKey("p" + i)) {
				l.add(query.remove("p" + i));
			} else {
				return l;
			}
		}
	}

	public void exec(RoutingService routing, RoutingParameters routingParms, ComponentMatcher target,
			Class<? extends Service> service, Class<? extends Endpoint> endpoint, EndpointParameterList parms,
			boolean compress, boolean encrypt, String duration, double timeout,
			Function<MessageCollector, Object> whatToSendF, Serializer serializer, OutputStream output,
			InputStream postDataInputStream) {

		final String description = routing.getFriendlyName() + "/" + routingParms.toURLElement() + "/" + target + "/"
				+ service.getName() + "/" + endpoint.getSimpleName() + "/" + TextUtilities.concat("/", parms)
				+ "?compress=" + compress + ",encrypt=" + encrypt + ",duration=" + duration + ",timeout=" + timeout
				+ ",what=" + name(whatToSendF) + ",format=" + serializer.getMIMEType();

		var ro = routing.exec(target, service, endpoint, routingParms, parms, postDataInputStream == null);
		var aes = new AES();
		SecretKey key = null;

		var collector = new MessageCollector(ro.returnQ);

		if (postDataInputStream != null) {
			new Thread(() -> {
				ObjectMapper jsonParser = new ObjectMapper();

				try {
					JsonNode header = jsonParser.readTree(postDataInputStream);

					if (header != null) { // EOF
						// the header may carry control commands for the collect process
						controlCollect(header, collector);

						int contentLength = header.get("contentLength").asInt();

						var content = postDataInputStream.readNBytes(contentLength);
						var encodingsFromClient = header.get("encodings").asText().split(",");
						// restore the original data by reversing the encoding
						for (int i = encodingsFromClient.length - 1; i > 0; --i) {
							var encoding = encodingsFromClient[i];

							if (encoding.equals("gzip")) {
								content = Utilities.gunzip(content);
							} else if (encoding.equals("base64")) {
								content = TextUtilities.unbase64(content);
							} else if (encoding.equals("aes")) {
								try {
									content = aes.decode(content, key);
								} catch (Exception e) {
									throw new IllegalStateException(e);
								}
							}
						}

						String serializerName = encodingsFromClient[0];
						var ser = name2serializer.get(serializerName);
						Object o = ser.fromBytes(content);
						routing.send(o, false, ro.getOperationInputQueueDestination());
					}

					routing.send(null, true, ro.getOperationInputQueueDestination());
				} catch (IOException e1) {
				}
			}).start();
		}

		System.out.println("collecting...");

		collector.collect(timeout, timeout, collecto -> {
			List<String> encodingsToClient = new ArrayList<>();
			Object what2send = whatToSendF.apply(collecto);

			var ser = what2send instanceof byte[] ? name2serializer.get("bytes") : serializer;
			var bytes = ser.toBytes(what2send);
			encodingsToClient.add(ser.getMIMEType());
			boolean base64 = ser.isBinary() || compress || encrypt;

			if (compress) {
				bytes = Utilities.gzip(bytes);
				encodingsToClient.add("gzip");
			}

			if (encrypt) {
				try {
					bytes = aes.encode(bytes, key);
					encodingsToClient.add("aes");
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}

			try {
				sendEvent(output, new ChunkHeader(encodingsToClient), bytes, base64);
			} catch (IOException e) {
				Cout.debugSuperVisible("Client has closed");
				collecto.stop = true;
				component.bb().send("stop", ro.destination);
			}
		});

		System.out.println("collecting completed");

//			ro.dispose();

	}

	private String name(Function<MessageCollector, Object> whatToSendF) {
		for (var e : whatToSendMap.entrySet()) {
			if (e.getValue() == whatToSendF) {
				return e.getKey();
			}
		}

		throw new IllegalStateException();
	}

	private void controlCollect(JsonNode header, MessageCollector collector) {
		var newTimeout = header.get("timeout");

		if (newTimeout != null) {
			collector.timeout = newTimeout.asDouble();
		}

		var newEndDate = header.get("endDate");

		if (newEndDate != null) {
			collector.endDate = newEndDate.asDouble();
		}

		var shouldStop = header.get("stop");

		if (shouldStop != null) {
			collector.stop = shouldStop.asBoolean();
		}
	}

	private List<String> path(String s) {
		if (s == null) {
			return null;
		}

		if (s.startsWith("/")) {
			s = s.substring(1);
		}

		// if the path ends by a /
		if (s.charAt(s.length() - 1) == '/' && s.charAt(s.length() - 2) != '/') {
			s = s.substring(0, s.length() - 1);
		}

		return s.isEmpty() ? null : TextUtilities.split(s, '/');
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

	private static Map<String, String> query(String s) {
		Map<String, String> query = new HashMap<>();

		if (s != null && !s.isEmpty()) {
			for (String queryEntry : TextUtilities.split(s, '&')) {
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

	public class stopHTTPServer extends TypedInnerClassEndpoint {
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

	public class startHTTPServerOperation extends TypedInnerClassEndpoint {
		public void f(int port) throws IOException {
			startHTTPServer(port);
		}

		@Override
		public String getDescription() {
			return "starts the HTTP server";
		}
	}
}
