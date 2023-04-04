package idawi.service.web;

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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import javax.crypto.SecretKey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import idawi.Component;
import idawi.InnerClassOperation;
import idawi.OperationParameterList;
import idawi.RemotelyRunningOperation;
import idawi.Service;
import idawi.TypedInnerClassOperation;
import idawi.knowledge_base.DigitalTwinService;
import idawi.messaging.MessageCollector;
import idawi.routing.BlindBroadcasting;
import idawi.routing.FloodingWithSelfPruning;
import idawi.routing.RoutingData;
import idawi.routing.RoutingService;
import idawi.routing.TargetComponents;
import idawi.service.DemoService;
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
import toools.text.TextUtilities;

public class WebService extends Service {

	public static int DEFAULT_PORT = 8081;
	public static Map<String, Serializer> name2serializer = new HashMap<>();
	public static final Map<String, Class<? extends Service>> friendyName_service = new HashMap<>();

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

	public Map<String, String> serviceShortcuts = new HashMap<>();

	public WebService(Component t) {
		super(t);

		serviceShortcuts.put("bb", BlindBroadcasting.class.getName());
		serviceShortcuts.put("dt", DigitalTwinService.class.getName());
		serviceShortcuts.put("fwsp", FloodingWithSelfPruning.class.getName());
		serviceShortcuts.put("demo", DemoService.class.getName());
	}

	public class Base64URL extends TypedInnerClassOperation {
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
			e.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
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
							err.printStackTrace();
							sendEvent(output, new ChunkHeader(List.of(serializer.getMIMEType())),
									serializer.toBytes(err), serializer.isBinary());
						} finally {
							sendEvent(output, new ChunkHeader(List.of(serializer.getMIMEType())), "EOT".getBytes(),
									false);
						}
					} else if (context.equals("favicon.ico")) {
						writeOneShot(HttpURLConnection.HTTP_OK, "image/x-icon",
								new JavaResource(WebService.class, "flavicon.ico").getByteArray(), e, output);
					} else if (context.equals("frontend")) {
						if (path.isEmpty()) {
							path.add(new JavaResource(getClass(), "web/index.html").getPath());
						}

						writeOneShot(HttpURLConnection.HTTP_OK, guessMIMEType(path),
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
			out.write(header.toJSONNode().toString().getBytes());
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
		boolean compress = Boolean.valueOf(removeOrDefault(query, "compress", "false", Set.of("true", "false")));
		boolean encrypt = Boolean.valueOf(removeOrDefault(query, "encrypt", "no", Set.of("yes", "no")));

		Map<String, Function<MessageCollector, Object>> whatToSendMap = new HashMap<>();
		whatToSendMap.put("msg", c -> c.messages.last());
		whatToSendMap.put("content", c -> c.messages.last().content);
		whatToSendMap.put("route", c -> c.messages.last().route);
		whatToSendMap.put("source", c -> c.messages.last().route.initialEmission.transport.component);
		whatToSendMap.put("sc", c -> new Object[] { c.messages.last().route.initialEmission.transport.component,
				c.messages.last().content });

		String whatToSendString = String.valueOf(removeOrDefault(query, "what", "msg", whatToSendMap.keySet()));
		var whatToSendF = whatToSendMap.get(whatToSendString);

		if (!query.isEmpty()) {
			throw new IllegalStateException("unused parameters: " + query.keySet().toString());
		}

		RoutingService routing = component.defaultRoutingProtocol();
		var routingString = path.isEmpty() ? "" : path.remove(0);

		if (!routingString.isEmpty()) {
			routingString = serviceShortcuts.getOrDefault(routingString, routingString);

			try {
				var routingClass = Class.forName(routingString);
				routing = (RoutingService) routingClass.getConstructor(Component.class).newInstance(component);
			} catch (Exception err) {
				throw new RuntimeException(err);
			}
		}

		RoutingData routingParms = routing.createDefaultRoutingParms();
		var routingDataDescr = path.isEmpty() ? "" : path.remove(0);

		if (!routingDataDescr.isEmpty()) {
			routingParms.fromString(routingDataDescr, routing);
		}

		TargetComponents target = TargetComponents.all;
		var targetString = path.isEmpty() ? "" : path.remove(0);

		if (!targetString.isEmpty()) {
			target = TargetComponents.fromString(targetString, component.lookup(DigitalTwinService.class));
		}

		Class<? extends Service> actualServiceID = DigitalTwinService.class;
		Class<? extends InnerClassOperation> operationID = DigitalTwinService.components.class;
		var serviceOperationString = path.isEmpty() ? "" : path.remove(0);

		if (!serviceOperationString.isEmpty()) {
			int p = serviceOperationString.indexOf('$');
			String serviceString;
			String operationString;

			if (p == -1) {
				serviceString = serviceOperationString;
				operationString = InnerClassOperation.name(operationID);
			} else {
				serviceString = serviceOperationString.substring(0, p);
				operationString = serviceOperationString.substring(p + 1);
			}

			Cout.debug(serviceShortcuts);
			serviceString = serviceShortcuts.getOrDefault(serviceString, serviceString);

			try {
				actualServiceID = (Class<? extends Service>) Class.forName(serviceString);
				var declaringServiceClass = actualServiceID;

				while (true) {
					try {
						var operationClassName = declaringServiceClass.getName() + "$" + operationString;
//						System.err.println("trying " + operationClassName);
						operationID = (Class<? extends InnerClassOperation>) Class.forName(operationClassName);
						break;
					} catch (Exception e) {
						// e.printStackTrace();
						// System.err.println("can't instantiate " + operationClassName);

						if (Service.class.isAssignableFrom(declaringServiceClass.getSuperclass())) {
							declaringServiceClass = (Class<? extends Service>) declaringServiceClass.getSuperclass();
						} else {
							throw new RuntimeException(
									"can find operation " + operationString + " in the hierarchy of " + serviceString);
						}
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		var parms = new OperationParameterList();
		parms.addAll(path);

		System.out.println(routing.getClass().getName() + "/" + routingParms + "/" + target + "/"
				+ operationID.getName() + "/" + TextUtilities.concat("/", parms) + "?" + query);

		RemotelyRunningOperation ro = routing.exec(operationID, routingParms, target, true, parms);
		var aes = new AESEncrypter();
		SecretKey key = null;

		ObjectMapper jsonParser = new ObjectMapper();
		final var routing2 = routing;

		// Cout.debugSuperVisible("collecting...");

		ro.returnQ.collect(duration, timeout, collector -> {
			// Cout.debugSuperVisible("result: " + collector.messages.last());
			List<String> encodingsToClient = new ArrayList<>();
			Object what2send = whatToSendF.apply(collector);
			Cout.debug("JSONizing " + what2send);

			var bytes = serializer.toBytes(what2send);
			encodingsToClient.add(serializer.getMIMEType());
			boolean base64 = serializer.isBinary() || compress || encrypt;

			if (compress) {
				bytes = Utilities.gzip(bytes);
				encodingsToClient.add("gzip");
			}

			if (encrypt) {
				try {
					bytes = aes.encrypt(bytes, key);
					encodingsToClient.add("aes");
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}

			sendEvent(output, new ChunkHeader(encodingsToClient), bytes, base64);

			if (postDataInputStream != null) {
				try {
					JsonNode header = jsonParser.readTree(postDataInputStream);

					if (header != null) { // EOF
						// the header may carry control stuff for the collect process
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
								content = unbase64(content);
							} else if (encoding.equals("aes")) {
								try {
									content = aes.decrypt(content, key);
								} catch (Exception e) {
									throw new IllegalStateException(e);
								}
							}
						}

						String serializerName = encodingsFromClient[0];
						var ser = name2serializer.get(serializerName);
						Object o = ser.fromBytes(content);
						routing2.send(o, ro.getOperationInputQueueDestination());
					}
				} catch (IOException e1) {
				}
			}
		});

//			ro.dispose();

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

	public class stopHTTPServer extends TypedInnerClassOperation {
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

	public class startHTTPServerOperation extends TypedInnerClassOperation {
		public void f(int port) throws IOException {
			startHTTPServer(port);
		}

		@Override
		public String getDescription() {
			return "starts the HTTP server";
		}
	}

}
