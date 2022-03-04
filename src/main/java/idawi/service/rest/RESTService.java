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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpServer;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Message;
import idawi.MessageCollector;
import idawi.MessageList;
import idawi.OperationParameterList;
import idawi.RegistryService;
import idawi.RemotelyRunningOperation;
import idawi.Service;
import idawi.ServiceDescriptor;
import idawi.Streams;
import idawi.To;
import idawi.TypedInnerOperation;
import idawi.net.JacksonSerializer;
import idawi.service.ServiceManager;
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
		name2serializer.put("json_gson", new GSONSerializer<>());
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
				e.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
				Map<String, String> query = query(uri.getQuery());
				processRequest(path, query, is, r -> {
					try {
						System.out.println("sending " + new String(r));
						out.write(r);
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
				serveAPI(path, query, is, output);
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

	private void serveAPI(List<String> path, Map<String, String> query, InputStream is, Consumer<byte[]> output)
			throws Throwable {
		Serializer serializer = ser(query.remove("format"));

		try {
			processRESTRequest(path, query, is, r -> {
				if (r == null) {
					r = new NULL();
				}

				output.accept(serializer.toBytes(r));
			});

			query.keySet().forEach(k -> output.accept(serializer.toBytes("unused parameter: " + k)));
			output.accept(serializer.toBytes("EOF"));
		} catch (Throwable e) {
			e.printStackTrace();
			RESTError err = new RESTError();
			err.msg = e.getMessage();
			err.type = Clazz.classNameWithoutPackage(e.getClass().getName());
			err.javaStackTrace = TextUtilities.exception2string(e);
			output.accept(serializer.toBytes(err));
		}
	}

	private Serializer ser(String format) {
		Serializer serializer = new GSONSerializer<>();

		if (format != null) {
			serializer = name2serializer.get(format);

			if (serializer == null) {
				throw new IllegalArgumentException(
						"unknown format: " + format + ". Available format are: " + name2serializer.keySet());
			}
		}

		return serializer;
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
		double timeout = Double.valueOf(query.getOrDefault("timeout", "1"));

		if (path == null || path.isEmpty()) {
			output.accept(component.descriptor());
		} else {
			Set<ComponentDescriptor> components = componentsFromURL(path.remove(0));

			// there's nothing more
			if (path.isEmpty()) {
				output.accept(describeComponent(components, timeout).toArray(new ComponentDescriptor[0]));
			} else {
				String serviceName = path.remove(0);
				Class<? extends Service> serviceID = Clazz.findClass(serviceName);

				if (serviceID == null) {
					throw new Error("service " + serviceName + " is not known");
				} else if (path.isEmpty()) {
					output.accept(decribeService(components, serviceID));
				} else {
					String operation = path.remove(0);
					var parms = new OperationParameterList();
					parms.addAll(path);

					System.out.println("calling operation " + components + "/" + serviceID.toString() + "/" + operation
							+ " with parameters: " + parms);
					var to = new To(components).s(serviceID).o(operation);

					Consumer<MessageCollector> c1 = c -> c.stop = c.messages.filter(m -> m.isEOT()).senders()
							.equals(components);

					AtomicInteger i = new AtomicInteger();
					Consumer<MessageCollector> c2 = c -> c.stop = i.incrementAndGet() == 3;

					RemotelyRunningOperation ro = exec(to, true, parms);

					if (is != null) {
						Streams.split(is, 1000, m -> ro.send(m));
					}

					MessageCollector c = ro.returnQ.collect(timeout, timeout, c1);
					MessageList r = c.messages.throwAnyError();

					var m = r.senderName2contents();
					output.accept(m);
				}
			}
		}
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

	private Set<ComponentDescriptor> describeComponent(Set<ComponentDescriptor> components, double timeout)
			throws Throwable {
		Set<ComponentDescriptor> r = new HashSet<>();
		var to = new To(components).o(ServiceManager.list.class);
		var res = exec(to, true, null).returnQ;

		for (var m : res.collect(timeout, timeout, c -> {
			c.stop = c.messages.senders().equals(components);
		}).messages.throwAnyError().resultMessages()) {
			ComponentDescriptor c = m.route.source().component;
			c.services = (Set<ServiceDescriptor>) m.content;
			r.add(c);
		}

		return r;
	}

	private Map<ComponentDescriptor, ServiceDescriptor> decribeService(Set<ComponentDescriptor> components,
			Class<? extends Service> serviceID) throws Throwable {
		Map<ComponentDescriptor, ServiceDescriptor> descriptors = new HashMap<>();
		var to = new To(components).s(serviceID).o(Service.DescriptorOperation.class);
		var res = exec(to, true, null).returnQ;

		for (Message m : res.collectUntilFirstEOT().throwAnyError().resultMessages()) {
			descriptors.put(m.route.source().component, (ServiceDescriptor) m.content);
		}

		return descriptors;
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
