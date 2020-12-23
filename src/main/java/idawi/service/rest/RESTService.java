package idawi.service.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Message;
import idawi.Operation;
import idawi.RemoteException;
import idawi.Service;
import idawi.ServiceDescriptor;
import idawi.To;
import idawi.net.JacksonSerializer;
import idawi.net.LMI;
import idawi.service.ServiceManager;
import idawi.service.registry.RegistryService;
import toools.io.JavaResource;
import toools.io.file.Directory;
import toools.io.file.RegularFile;
import toools.io.ser.FSTSerializer;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;
import toools.reflect.Clazz;
import toools.text.TextUtilities;
import toools.thread.Threads;

public class RESTService extends Service {
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
	}

	public RESTService(Component t) {
		super(t);
	}

	public HttpServer startHTTPServer() throws IOException {
		return startHTTPServer(8080);
	}

	public HttpServer startHTTPServer(int port) throws IOException {
		if (restServer != null) {
			throw new IOException("REST server is already running");
		}

		restServer = HttpServer.create(new InetSocketAddress("localhost", port), 0);
		restServer.createContext("/", e -> {
			try {
				URI uri = e.getRequestURI();
				List<String> path = path(uri.getPath());
				Map<String, String> query = query(uri.getQuery());
				processRequest(path, query, bytes -> sendBack(bytes, e));
			} catch (Throwable err) {
				err.printStackTrace();
			}
		});
		restServer.setExecutor(Service.threadPool);
		restServer.start();
		return restServer;
	}

	private void sendBack(byte[] o, HttpExchange e) {
		try {
			e.sendResponseHeaders(200, o.length);
			OutputStream out = e.getResponseBody();

			if (e.getRequestMethod().equals("GET")) {
				out.write(o);
			}

			out.close();
		} catch (IOException t) {
			error(t);
		}
	}

	private void processRequest(List<String> path, Map<String, String> query, Consumer<byte[]> out) throws Throwable {
		if (path == null) {
			out.accept(new JavaResource(getClass(), "root.html").getByteArray());
		} else {
			String context = path.remove(0);

			if (context.equals("api")) {
				processAPI(path, query, out);
			} else if (context.equals("file")) {
				serveFiles(path, query, out);
			} else if (context.equals("idaweb")) {
				serveIdaweb(path, query, out);
			}
		}
	}

	private void serveIdaweb(List<String> path, Map<String, String> query, Consumer<byte[]> out) throws Throwable {
		if (path.isEmpty()) {
			out.accept(new JavaResource(getClass(), "index.html").getByteArray());
		} else {
			out.accept(new JavaResource(getClass(), TextUtilities.concatene(path, "/")).getByteArray());
		}
	}

	private void serveFiles(List<String> path, Map<String, String> query, Consumer<byte[]> out) throws Throwable {
		out.accept(new RegularFile(Directory.getHomeDirectory(), TextUtilities.concatene(path, "/")).getContent());
	}

	private void processAPI(List<String> path, Map<String, String> query, Consumer<byte[]> out) throws Throwable {
		Serializer serializer = new GSONSerializer<>();

		if (query.containsKey("format")) {
			String format = query.get("format");
			serializer = name2serializer.get(format);

			if (serializer == null) {
				out.accept(("unknow format: " + format + ". Available format are: " + name2serializer.keySet())
						.getBytes());
			}
		}

		try {
			Object result = processRESTRequest(path, query);
			out.accept(serializer.toBytes(result));
		} catch (Throwable t) {
			out.accept(serializer.toBytes(t));
			throw t;
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

	private Object processRESTRequest(List<String> path, Map<String, String> query) throws Throwable {
		double timeout = Double.valueOf(query.getOrDefault("timeout", "1"));

		if (path == null || path.isEmpty()) {
			return welcomePage();
		} else {
			Set<ComponentInfo> components = componentsFromURL(path.get(0));

			if (path.size() == 1) {
				return describeComponent(components, timeout).toArray(new ComponentInfo[0]);
			} else {
				String serviceName = path.get(1);
				Class<? extends Service> serviceID = Clazz.findClass(serviceName);

				if (serviceID == null) {
					throw new Error("service " + serviceName + " is not known");
				} else if (path.size() == 2) {
					return decribeService(components, serviceID);
				} else {
					String operation = path.get(2);

					if (path.size() > 4) {
						throw new Error("path too long! Expecting: component/service/operation");
					} else {
						var stringParms = path.size() == 3 ? new String[0] : path.get(3).split(",");
						System.out.println("calling operation " + components + "/" + serviceID.toString() + "/"
								+ operation + "(" + Arrays.toString(stringParms) + ")");
						List<Object> r = call(new To(components, serviceID, operation), stringParms).setTimeout(timeout)
								.collect().throwAnyError().resultMessages().contents();

						if (r.size() == 1) {
							return r.get(0);
						} else {
							return r;
						}
					}
				}
			}
		}
	}

	private Set<ComponentInfo> componentsFromURL(String s) {
		if (s.isEmpty()) {
			return null;
		}

		Set<ComponentInfo> components = new HashSet<>();

		for (String name : s.split(",")) {
			var found = service(RegistryService.class).lookup(name);

			if (found == null) {
				components.add(ComponentInfo.fromCDL("name=" + name));
			} else {
				components.add(found);
			}
		}

		return components;
	}

	private Set<ComponentInfo> describeComponent(Set<ComponentInfo> components, double timeout) throws Throwable {
		Set<ComponentInfo> r = new HashSet<>();

		for (var m : call(new To(components, ServiceManager.class, "list")).setTimeout(timeout).collect()
				.throwAnyError().resultMessages()) {
			ComponentInfo c = m.route.source().component;
			c.services = (Set<String>) m.content;
			r.add(c);
		}

		return r;
	}

	private Map<ComponentInfo, ServiceDescriptor> decribeService(Set<ComponentInfo> components,
			Class<? extends Service> serviceID) throws Throwable {
		Map<ComponentInfo, ServiceDescriptor> descriptors = new HashMap<>();

		for (Message m : call(new To(components, serviceID, "descriptor")).collect().throwAnyError().resultMessages()) {
			descriptors.put(m.route.source().component, (ServiceDescriptor) m.content);
		}

		return descriptors;
	}

	public static class Welcome {
		String localComponent;
		Set<ComponentInfo> knownComponents = new HashSet<>();
	}

	private Welcome welcomePage() throws Throwable {
		Welcome w = new Welcome();
		w.localComponent = component.descriptor().friendlyName;

		// asks all components to send their descriptor, which will be catched by the
		// networking service
		// that will pass it to the registry
		var q = call(new To(RegistryService.class, "local"));
		q.setTimeout(1).collect();

		w.knownComponents.addAll(service(RegistryService.class).descriptors());
		return w;
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

	@Operation
	public void stopHTTPServer() throws IOException {
		if (restServer == null) {
			throw new IOException("REST server is not running");
		}

		restServer.stop(0);
		restServer = null;
	}

	@Operation
	private void startHTTPServerOperation(int port) throws IOException {
		startHTTPServer(port);
	}

	public static void main(String[] args) throws IOException, RemoteException {
		List<Component> components = new ArrayList();

		for (int i = 0; i < 20; ++i) {
			components.add(new Component());
		}

//		LMI.randomTree(components);
		LMI.chain(components);
		components.get(0).lookupService(RESTService.class).startHTTPServer();

		Threads.sleepForever();
	}
}
