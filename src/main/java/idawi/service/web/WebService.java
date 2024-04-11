package idawi.service.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
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
import com.sun.net.httpserver.HttpServer;

import idawi.Component;
import idawi.Endpoint;
import idawi.EndpointParameterList;
import idawi.Idawi;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.MessageCollector;
import idawi.messaging.RoutingStrategy;
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
	private Map<String, Function<MessageCollector, Object>> whatToSendMap = new HashMap<>();
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
			List<String> path = Utils.path(uri.getPath());
			Map<String, String> query = Utils.query(uri.getQuery());
			e.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			OutputStream output = e.getResponseBody();

			try {
				if (path == null) {
					Utils.singleHTTPResponse(HttpURLConnection.HTTP_OK, "text/html",
							new JavaResource(getClass(), "root.html").getByteArray(), e, output);
				} else {
					String context = path.remove(0);

					if (context.equals("api")) {
						// setting default serializer
						Serializer preferredSerializer = name2serializer.get("jaseto");

						try {
							e.getResponseHeaders().set("Content-type", "text/event-stream");
							e.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
							var preferredOuputFormat = Utils.removeOrDefault(query, "format",
									preferredSerializer.getMIMEType(), name2serializer.keySet());
							preferredSerializer = name2serializer.get(preferredOuputFormat);
							serveAPI(path, query, input, output, preferredSerializer);
							Utils.sendEvent(output, new ChunkHeader(List.of("plain")), "EOT".getBytes(), false);
						} catch (Throwable err) {
							err.printStackTrace();
							try {
								Utils.sendEvent(output, new ChunkHeader(List.of(preferredSerializer.getMIMEType())),
										preferredSerializer.toBytes(err), preferredSerializer.isBinary());
								Utils.sendEvent(output, new ChunkHeader(List.of("plain")), "EOT".getBytes(), false);
							} catch (IOException ioerr) {

							}
						}
					} else if (context.equals("favicon.ico")) {
						Utils.singleHTTPResponse(HttpURLConnection.HTTP_OK, "image/x-icon",
								new JavaResource(WebService.class, "flavicon.ico").getByteArray(), e, output);
					} else if (context.equals("frontend")) {
						if (path.isEmpty()) {
							path.add(new JavaResource(getClass(), "web/index.html").getPath());
						}

						Utils.singleHTTPResponse(HttpURLConnection.HTTP_OK, Utils.guessMIMEType(path),
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

						Utils.singleHTTPResponse(HttpURLConnection.HTTP_OK, Utils.guessMIMEType(path), bytes, e,
								output);
					} else {
						throw new IllegalArgumentException("unknown context: " + context);
					}
				}
			} catch (Throwable err) {
				try {
					System.err.println("The following error will be sent to the Web client");
					err.printStackTrace();
					Utils.singleHTTPResponse(HttpURLConnection.HTTP_INTERNAL_ERROR, "text/plain",
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
		return httpServer;
	}

	private void serveAPI(List<String> path, Map<String, String> query, InputStream postDataInputStream,
			OutputStream output, Serializer serializer) throws IOException, ClassNotFoundException {

		if (!query.containsKey("enough")) {
			new Suggestion("enough", "describe a termination condition", null).send(output, serializer);
		}

		Predicate<MessageCollector> stopCollectingWhen = Utils
				.parseGotEnoughCondition(Utils.removeOrDefault(query, "enough", null, null));
		double timeout = Double.valueOf(Utils.removeOrDefault(query, "timeout", "1", null));
		boolean compress = Boolean.valueOf(Utils.removeOrDefault(query, "compress", "false", Set.of("true", "false")));
		boolean encrypt = Boolean.valueOf(Utils.removeOrDefault(query, "encrypt", "no", Set.of("yes", "no")));
		var whatToSendF = whatToSendMap
				.get(String.valueOf(Utils.removeOrDefault(query, "what", "msg", whatToSendMap.keySet())));

		final RoutingService<?> routing = routing(query, output, serializer);
		final RoutingParameters routingParms = Utils.routingParms(query, routing, output, serializer);
		final var target = matcher(query, output, routing, routingParms, serializer);
		final var serviceClass = service(query, output, serializer, target, routing, routingParms, stopCollectingWhen);

		if (serviceClass == null)
			return;

		final var endpointClass = endpoint(query, output, serializer, target, routing, routingParms, serviceClass,
				stopCollectingWhen);

		if (endpointClass == null)
			return;

		final EndpointParameterList parms = parmsFromQuery(query);

		if (!query.isEmpty()) {
			throw new IllegalStateException("unused parameters: " + query.keySet().toString());
		}

		var remoteEndpoint = routing.exec(target, serviceClass, endpointClass, parms, msg -> {
			msg.initialRoutingStrategy.parms = routingParms;
			msg.eot = postDataInputStream == null;
		});

		var aes = new AES();
		SecretKey key = null;

		var collector = new MessageCollector(remoteEndpoint.returnQ);

		if (postDataInputStream != null) {
			new Thread(() -> {
				ObjectMapper jsonParser = new ObjectMapper();

				try {
					JsonNode header = jsonParser.readTree(postDataInputStream);

					if (header != null) { // EOF
						// the header may carry control commands for the collect process
						Utils.controlCollect(header, collector);

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
						routing.send(o, remoteEndpoint.inputQAddr);
					}

					routing.send(null, remoteEndpoint.inputQAddr, msg -> msg.eot = true);
				} catch (IOException e1) {
				}
			}).start();
		}

		System.out.println("collecting...");

		collector.collect(timeout, collecto -> {
			collecto.gotEnough = stopCollectingWhen.test(collecto);
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
				Utils.sendEvent(output, new ChunkHeader(encodingsToClient), bytes, base64);
			} catch (IOException e) {
				Cout.debugSuperVisible("Client has closed");
				collecto.gotEnough = true;
				component.bb().send("stop", remoteEndpoint.inputQAddr);
			}
		});

		System.out.println("collecting completed");
	}

	private Class<? extends Endpoint> endpoint(Map<String, String> query, OutputStream output, Serializer serializer,
			ComponentMatcher matcher, RoutingService<?> r, RoutingParameters rp, Class<? extends Service> s,
			Predicate<MessageCollector> stopCollectingWhen) throws ClassNotFoundException, IOException {
		if (query.containsKey("e")) {
			return (Class<? extends Endpoint>) Clazz.innerClass(s, query.remove("e"));
		} else {
			var ro = r.exec(matcher, s, listEndpoints.class, null, msg -> {
				msg.eot = true;
				msg.initialRoutingStrategy = new RoutingStrategy(r, rp);
			});
			var messages = ro.returnQ.collector().collectUntil(stopCollectingWhen).messages;
			var map = new HashMap<Component, List<String>>();
			messages.forEach(m -> map.put(m.sender(),
					((Set<Class<? extends Endpoint>>) m.content).stream().map(e -> e.getSimpleName()).toList()));
			new Suggestion("e", "gives the name of an endpoint", map).send(output, serializer);
			return null;
		}
	}

	private Class<? extends Service> service(Map<String, String> query, OutputStream output, Serializer serializer,
			ComponentMatcher t, RoutingService<?> r, RoutingParameters rp,
			Predicate<MessageCollector> terminationCondition) throws ClassNotFoundException, IOException {
		if (query.containsKey("s")) {
			var s = query.remove("s");
			var c = shortcut_service.get(s);

			if (c != null) {
				return c;
			} else {
				return (Class<? extends Service>) Class.forName(s);
			}
		} else {
			var ro = r.exec(t, ServiceManager.class, listServices.class, null, msg -> {
				msg.eot = true;
				msg.initialRoutingStrategy = new RoutingStrategy(r, rp);
			});
			var messages = ro.returnQ.collector().collectUntil(terminationCondition).messages;
			var map = new HashMap<Component, List<? extends Service>>();
			messages.forEach(m -> map.put(m.sender(), (List<? extends Service>) m.content));
			new Suggestion("s", "gives the name of a service", map).send(output, serializer);
			return null;
		}
	}

	private ComponentMatcher matcher(Map<String, String> query, OutputStream output, RoutingService r,
			RoutingParameters rp, Serializer serializer) throws IOException {
		if (query.containsKey("t")) {
			return ComponentMatcher.fromString(query.remove("t"), component.localView());
		} else {
			new Suggestion("t", "specifies the components targetted", component.localView().g.components()).send(output,
					serializer);
			return ComponentMatcher.all;
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
			new Suggestion("r", "gives the name of an routing algorithm",
					component.services(RoutingService.class).stream().map(s -> s.getClass()).toList())
					.send(output, serializer);
			return component.defaultRoutingProtocol();
		}
	}

	private static EndpointParameterList parmsFromQuery(Map<String, String> query) {
		var l = new EndpointParameterList();

		for (int i = 0;; ++i) {
			if (query.containsKey("p" + i)) {
				l.add(query.remove("p" + i));
			} else {
				return l;
			}
		}
	}

	private String name(Function<MessageCollector, Object> whatToSendF) {
		for (var e : whatToSendMap.entrySet()) {
			if (e.getValue() == whatToSendF) {
				return e.getKey();
			}
		}

		throw new IllegalStateException();
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
