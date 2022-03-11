package idawi.service.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpServer;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.InnerOperation;
import idawi.MessageCollector;
import idawi.OperationAddress;
import idawi.OperationParameterList;
import idawi.RegistryService;
import idawi.RemotelyRunningOperation;
import idawi.Service;
import idawi.Streams;
import idawi.To;
import idawi.TypedInnerOperation;
import idawi.net.JacksonSerializer;
import toools.io.JavaResource;
import toools.io.file.Directory;
import toools.io.file.RegularFile;
import toools.io.ser.FSTSerializer;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;
import toools.io.ser.TOMLSerializer;
import toools.io.ser.YAMLSerializer;
import toools.reflect.Clazz;
import toools.text.TextUtilities;

public class RESTService extends Service {
	public static int DEFAULT_PORT = 8081;

	private HttpServer restServer;

	private int port;
	public static Map<String, Serializer> name2serializer = new HashMap<>();

	static {
		name2serializer.put("gson", new GSONSerializer<>());
		name2serializer.put("json_jackson", new JacksonSerializer<>());
		name2serializer.put("ser", new JavaSerializer<>());
		name2serializer.put("fst", new FSTSerializer<>());
		name2serializer.put("xml", new XMLSerializer<>());
		name2serializer.put("toString", new ToStringSerializer<>());
		name2serializer.put("error", new StrackTraceSerializer<>());
		name2serializer.put("bytes", new ToBytesSerializer<>());
		name2serializer.put("toml", new TOMLSerializer<>());
		name2serializer.put("yaml", new YAMLSerializer<>());
	}

	public RESTService(Component t) {
		super(t);
	}

	@Override
	public void dispose() {
		super.dispose();
		restServer.stop(0);
	}

	public int getPort() {
		return port;
	}

	public HttpServer startHTTPServer() throws IOException {
		return startHTTPServer(DEFAULT_PORT);
	}

	public HttpServer startHTTPServer(int port) throws IOException {

		if (restServer != null) {
			throw new IOException("REST server is already running");
		}

		restServer = HttpServer.create(new InetSocketAddress(port), 0);
		restServer.createContext("/", e -> {
			URI uri = e.getRequestURI();
			InputStream is = "POST".equals(e.getRequestMethod()) ? e.getRequestBody() : null;
			// is.close();
			// Cout.debugSuperVisible(data.length);
			List<String> path = path(uri.getPath());
			e.getResponseHeaders().add("Access-Control-Allow-Origin:", "*");
			OutputStream out = e.getResponseBody();

			try {
				e.getResponseHeaders().set("Content-type", "idawi");
				e.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
				Map<String, String> query = query(uri.getQuery());
				processRequest(path, query, is, r -> {
					try {
						out.write(r);
						System.out.println("sending " + new String(r));
					} catch (IOException e1) {
						// cannot write anything back
						// at least show in the server's console
						e1.printStackTrace();
					}
				});
			} catch (Throwable err) {
//				Cout.debugSuperVisible(path + "   sending 404");
				var er = TextUtilities.exception2string(err).getBytes();
				e.sendResponseHeaders(HttpURLConnection.HTTP_OK, er.length);
				out.write(er);
				logError(err.getMessage());
				err.printStackTrace();
			}

			out.close();
		});
		restServer.setExecutor(Service.threadPool);
		restServer.start();
//		WSS webSocketServer = new WSS(8001, new GenerationData());
//		webSocketServer.start();
		this.port = port;
		System.out.println("Web server running on port " + port);
		return restServer;
	}

	private synchronized void processRequest(List<String> path, Map<String, String> query, InputStream is,
			Consumer<byte[]> output) throws Throwable {
		if (path == null) {
			output.accept(new JavaResource(getClass(), "root.html").getByteArray());
		} else {
//			Cout.debugSuperVisible("path: " + path);
			String context = path.remove(0);

			if (context.equals("api")) {
				serveAPI(path, query, is, (type, bytes) -> {
					output.accept((type + "\n").getBytes());
					output.accept((bytes.length + "\n").getBytes());
					output.accept(bytes);
				});
			} else if (context.equals("file")) {
				serveFiles(path, query, output);
			} else if (context.equals("favicon.ico")) {
				output.accept(new JavaResource(RESTService.class, "flavicon.ico").getByteArray());
			} else if (context.equals("web")) {
				serveWeb(path, query, output);
			} else {
				throw new IllegalArgumentException("unknown context: " + context);
			}
		}
	}

	private void serveWeb(List<String> path, Map<String, String> query, Consumer<byte[]> output) throws Throwable {
		if (path.isEmpty()) {
			output.accept(new JavaResource(getClass(), "web/index.html").getByteArray());
		} else {
			var res = new JavaResource("/" + TextUtilities.concatene(path, "/"));
			// Cout.debugSuperVisible("sending " + res.getName());
			output.accept(res.getByteArray());
		}
	}

	private void serveFiles(List<String> path, Map<String, String> query, Consumer<byte[]> output) throws Throwable {
		var i = new RegularFile(Directory.getHomeDirectory(), TextUtilities.concatene(path, "/")).createReadingStream();
		byte[] b = new byte[1024];

		while (true) {
			int n = i.read(b);

			if (n == -1) {
				return;
			} else if (n > 0) {
				output.accept(n == b.length ? b : Arrays.copyOf(b, n));
			}
		}
	}

	public static class Response {
		public List<RESTError> errors = new ArrayList<>();
		public List<String> warnings = new ArrayList<>();
		public List<Object> results = new ArrayList<>();

		@Override
		public String toString() {
			Map<String, List> m = new HashMap<>();
			m.put("errors", errors);
			m.put("warnings", warnings);
			m.put("results", results);
			return m.toString();
		}
	}

	public static interface BackToClient {
		void send(String contentType, byte[] data);
	}

	private void serveAPI(List<String> path, Map<String, String> query, InputStream is, BackToClient output)
			throws Throwable {
		var format = removeOrDefault(query, "format", "gson");
		Serializer serializer = name2serializer.get(format);

		try {
			processRESTRequest(path, query, is, r -> {
				if (r == null) {
					r = new NULL();
				}

				var bytes = serializer.toBytes(r);
				output.send(format, bytes);
			});

			query.keySet().forEach(k -> output.send("text", serializer.toBytes("unused parameter: " + k)));
		} catch (Throwable e) {
			e.printStackTrace();
			RESTError err = new RESTError();
			err.msg = e.getMessage();
			err.type = Clazz.classNameWithoutPackage(e.getClass().getName());
			err.javaStackTrace = TextUtilities.exception2string(e);
			output.send("error", serializer.toBytes(err));
		}
	}


	public static class RESTError implements Serializable {
		public String msg;
		public String type;
		public String javaStackTrace;
	}

	public static class NULL implements Serializable {
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

	private void processRESTRequest(List<String> path, Map<String, String> query, InputStream is,
			Consumer<Object> output) throws Throwable {
		double timeout = Double.valueOf(removeOrDefault(query, "timeout", "1"));
		double duration = Double.valueOf(removeOrDefault(query, "duration", "1"));

		// no target component are specified: let's consider the local one
		if (path == null || path.isEmpty()) {
			output.accept(component.descriptor());
		} else {
			Set<ComponentDescriptor> targets = componentsFromURL(path.remove(0));

			// there's nothing more
			if (path.isEmpty()) {
				path.add(RegistryService.class.getName());
				path.add(InnerOperation.name(RegistryService.local.class));
			}

			String serviceName = path.remove(0);
			Class<? extends Service> serviceID = Clazz.findClass(serviceName);

			if (serviceID == null)
				throw new Error("service " + serviceName + " is not known");

			if (path.isEmpty()) {
				path.add(InnerOperation.name(Service.DescriptorOperation.class));
			}

			String operation = path.remove(0);
			var parms = new OperationParameterList();
			parms.addAll(path);

			System.out.println("calling operation " + targets + "/" + serviceID.toString() + "/" + operation
					+ " with parameters: " + parms);
			var to = new To(targets).s(serviceID).o(operation);

			RemotelyRunningOperation ro = exec(to, true, parms);

			if (is != null) {
				Streams.split(is, 1000, m -> ro.send(m));
			}

			var stop = stoppers.get(removeOrDefault(query, "stop", "aeot"));

			if (!query.isEmpty()) {
				output.accept("unused parameters: " + query.keySet());
			}

			ro.returnQ.collect(duration, timeout, c -> {
				output.accept(c.messages.last());
				c.stop = stop.test(to, c);
			});
		}
	}

	private String removeOrDefault(Map<String, String> map, String k, String d) {
		var r = map.remove(k);
		return r == null ? d : r;
	}

	public static final Map<String, BiPredicate<OperationAddress, MessageCollector>> stoppers = new HashMap<>();

	static {
		stoppers.put("aeot", (to, c) -> c.messages.filter(m -> m.isEOT()).senders().equals(to.sa.to.componentIDs));
		stoppers.put("1eot", (to, c) -> !c.messages.filter(m -> m.isEOT()).isEmpty());
		stoppers.put("1m", (to, c) -> !c.messages.isEmpty());
		stoppers.put("1r", (to, c) -> !c.messages.filter(m -> m.isResult()).isEmpty());
	}

	private Set<ComponentDescriptor> componentsFromURL(String s) {
		if (s.isEmpty()) {
			return null;
		}

		Set<ComponentDescriptor> components = new HashSet<>();

		for (String name : s.split(",")) {
			var found = component.lookupO(RegistryService.lookUp.class).f(name);

			if (found == null) {
				components.add(ComponentDescriptor.fromCDL("name=" + name));
			} else {
				components.add(found);
			}
		}

		return components;
	}

	private Map<String, String> query(String s) {
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
			if (restServer == null) {
				throw new IOException("REST server is not running");
			}

			restServer.stop(0);
			restServer = null;
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class startHTTPServerOperation extends TypedInnerOperation {
		public void f(int port) throws IOException {
			startHTTPServer(port);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
