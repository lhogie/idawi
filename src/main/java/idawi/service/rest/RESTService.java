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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import idawi.Component;
import idawi.To;
import idawi.ComponentDescriptor;
import idawi.TypedOperation;
import idawi.Message;
import idawi.OperationParameterList;
import idawi.RegistryService;
import idawi.Service;
import idawi.ServiceDescriptor;
import idawi.net.JacksonSerializer;
import idawi.service.ServiceManager;
import toools.io.JavaResource;
import toools.io.file.Directory;
import toools.io.file.RegularFile;
import toools.io.ser.FSTSerializer;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;
import toools.reflect.Clazz;
import toools.text.TextUtilities;

public class RESTService extends Service {
	public static int DEFAULT_PORT = 8081;

	private HttpServer restServer;
	public static Map<String, Serializer> name2serializer = new HashMap<>();

	static {
		name2serializer.put("json_gson", new GSONSerializer<>());
		name2serializer.put("json_jackson", new JacksonSerializer());
		name2serializer.put("ser", new JavaSerializer<>());
		name2serializer.put("fst", new FSTSerializer<>());
		name2serializer.put("xml", new XMLSerializer<>());
		name2serializer.put("toString", new ToStringSerializer<>());
		name2serializer.put("error", new StrackTraceSerializer<>());
		name2serializer.put("bytes", new ToBytesSerializer());
	}

	public RESTService(Component t) {
		super(t);
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
			InputStream is = e.getRequestBody();
			var data = is.readAllBytes();
			is.close();
			// Cout.debugSuperVisible(data.length);
			List<String> path = path(uri.getPath());

			try {
				Map<String, String> query = query(uri.getQuery());
				var response = processRequest(path, query, data);
				sendBack(HttpURLConnection.HTTP_OK, response, e);
			} catch (Throwable err) {
//				Cout.debugSuperVisible(path + "   sending 404");
//				sendBack(HttpURLConnection.HTTP_NOT_FOUND, TextUtilities.exception2string(err).getBytes(), e);
				err.printStackTrace();
			}
		});
		restServer.setExecutor(Service.threadPool);
		restServer.start();
//		WSS webSocketServer = new WSS(8001, new GenerationData());
//		webSocketServer.start();

		System.out.println("Web server running on port " + port);
		return restServer;
	}

	private void sendBack(int returnCode, byte[] o, HttpExchange e) {
		try {
			e.getResponseHeaders().add("Access-Control-Allow-Origin:", "*");
			e.sendResponseHeaders(returnCode, o.length);
			OutputStream out = e.getResponseBody();
			// Cout.debug("sending " + o.length + " bytes");
			out.write(o);
			out.close();
		} catch (IOException t) {
			logError(t);
		}
	}

	private synchronized byte[] processRequest(List<String> path, Map<String, String> query, byte[] data)
			throws Throwable {
		if (path == null) {
			return new JavaResource(getClass(), "root.html").getByteArray();
		} else {
//			Cout.debugSuperVisible("path: " + path);
			String context = path.remove(0);

			if (context.equals("api")) {
				return serveAPI(path, query, data);
			} else if (context.equals("file")) {
				return serveFiles(path, query);
			} else if (context.equals("favicon.ico")) {
				return new JavaResource(RESTService.class, "flavicon.ico").getByteArray();
			} else if (context.equals("web")) {
				return serveWeb(path, query);
			} else {
				throw new IllegalArgumentException("unknown context: " + context);
			}
		}
	}

	private byte[] serveWeb(List<String> path, Map<String, String> query) throws Throwable {
		if (path.isEmpty()) {
			return new JavaResource(getClass(), "web/index.html").getByteArray();
		} else {
			var res = new JavaResource("/" + TextUtilities.concatene(path, "/"));
			// Cout.debugSuperVisible("sending " + res.getName());
			return res.getByteArray();
		}
	}

	private byte[] serveFiles(List<String> path, Map<String, String> query) throws Throwable {
		return new RegularFile(Directory.getHomeDirectory(), TextUtilities.concatene(path, "/")).getContent();
	}

	public static class Response {
		List<RESTError> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		List<Object> results = new ArrayList<>();

		@Override
		public String toString() {
			Map<String, List> m = new HashMap<>();
			m.put("errors", errors);
			m.put("warnings", warnings);
			m.put("results", results);
			return m.toString();
		}
	}

	private byte[] serveAPI(List<String> path, Map<String, String> query, byte[] data) throws Throwable {
		Response r = new Response();
		String only;
		Serializer serializer = new GSONSerializer<>();

		try {

			String format = query.remove("format");

			if (format != null) {
				serializer = name2serializer.get(format);
			}

			if (serializer == null) {
				throw new IllegalArgumentException(
						"unknown format: " + format + ". Available format are: " + name2serializer.keySet());
			}

			Object result = processRESTRequest(path, query, data);

			if (result == null) {
				result = new NULL();
			}

			r.results.add(result);
			only = query.remove("only");
			query.keySet().forEach(k -> r.warnings.add("unused parameter: " + k));

			if (only == null) {
				return serializer.toBytes(r);
			} else if (only.equals("errors")) {
				return serializer.toBytes(r.errors);
			} else if (only.equals("warnings")) {
				return serializer.toBytes(r.warnings);
			} else if (only.equals("results")) {
				return serializer.toBytes(r.results.get(0));
			} else {
				throw new IllegalArgumentException("unknown value for 'only': " + only
						+ ". Available format are 'errors', 'warnings' or 'results'");
			}
		} catch (Throwable e) {
			RESTError err = new RESTError();
			err.msg = e.getMessage();
			err.type = Clazz.classNameWithoutPackage(e.getClass().getName());
			err.javaStackTrace = TextUtilities.exception2string(e);
			r.errors.add(err);
			return serializer.toBytes(r);
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

	private Object processRESTRequest(List<String> path, Map<String, String> query, byte[] data) throws Throwable {
		double timeout = Double.valueOf(query.getOrDefault("timeout", "1"));

		if (path == null || path.isEmpty()) {
			return component.descriptor();
		} else {
			Set<ComponentDescriptor> components = componentsFromURL(path.remove(0));

			// there's nothing more
			if (path.isEmpty()) {
				return describeComponent(components, timeout).toArray(new ComponentDescriptor[0]);
			} else {
				String serviceName = path.remove(0);
				Class<? extends Service> serviceID = Clazz.findClass(serviceName);

				if (serviceID == null) {
					throw new Error("service " + serviceName + " is not known");
				} else if (path.isEmpty()) {
					return decribeService(components, serviceID);
				} else {
					String operation = path.remove(0);
					var parms = new OperationParameterList();
					parms.addAll(path);

					if (data != null && data.length > 0) {
						// POST data is always passed as the last parameter
						parms.add(data);
					}

					System.out.println("calling operation " + components + "/" + serviceID.toString() + "/" + operation
							+ " with parameters: " + parms);
					var to = new To(components).s(serviceID).o(operation);
					List<Object> r = exec(to, true, parms).returnQ.collect().throwAnyError_Runtime().contents();

					if (r.size() == 1) {
						return r.get(0);
					} else {
						return r;
					}

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
			var found = lookup(RegistryService.lookUp.class).lookup(name);

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

		for (var m : res.setMaxWaitTimeS(timeout).collect().throwAnyError().resultMessages()) {
			ComponentDescriptor c = m.route.source().component;
			c.servicesNames = (Set<String>) m.content;
			r.add(c);
		}

		return r;
	}

	private Map<ComponentDescriptor, ServiceDescriptor> decribeService(Set<ComponentDescriptor> components,
			Class<? extends Service> serviceID) throws Throwable {
		Map<ComponentDescriptor, ServiceDescriptor> descriptors = new HashMap<>();
		var to = new To(components).s(serviceID).o(Service.DescriptorOperation.class);
		var res = exec(to, true, null).returnQ;

		for (Message m : res.collect().throwAnyError().resultMessages()) {
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

	public class stopHTTPServer extends TypedOperation{
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

	public class startHTTPServerOperation extends TypedOperation{
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
