package idawi.service.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
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

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.IdawiOperation;
import idawi.Message;
import idawi.OperationParameterList;
import idawi.OperationStringParameterList;
import idawi.RegistryService;
import idawi.Service;
import idawi.ServiceAddress;
import idawi.ServiceDescriptor;
import idawi.net.JacksonSerializer;
import idawi.service.ServiceManager;
import toools.io.Cout;
import toools.io.JavaResource;
import toools.io.file.Directory;
import toools.io.file.RegularFile;
import toools.io.ser.FSTSerializer;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;
import toools.reflect.Clazz;
import toools.text.TextUtilities;

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

	public static int DEFAULT_PORT = 8081;

	public HttpServer startHTTPServer() throws IOException {
		return startHTTPServer(DEFAULT_PORT);
	}

	public HttpServer startHTTPServer(int port) throws IOException {
		if (restServer != null) {
			throw new IOException("REST server is already running");
		}

		restServer = HttpServer.create(new InetSocketAddress("localhost", port), 0);
		restServer.createContext("/", e -> {
			URI uri = e.getRequestURI();
			List<String> path = path(uri.getPath());
			try {
				Map<String, String> query = query(uri.getQuery());
				var response = processRequest(path, query);
				sendBack(200, response, e);
			} catch (Throwable err) {
				Cout.debugSuperVisible(path + "   sending 404");
				sendBack(404, TextUtilities.exception2string(err).getBytes(), e);
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
			e.sendResponseHeaders(returnCode, o.length);
			OutputStream out = e.getResponseBody();

			if (e.getRequestMethod().equals("GET")) {
				Cout.debug("sending " + o.length + "  bytes");
				out.write(o);
			}

			out.close();
		} catch (IOException t) {
			error(t);
		}
	}

	private synchronized byte[] processRequest(List<String> path, Map<String, String> query) throws Throwable {
		if (path == null) {
			return new JavaResource(getClass(), "root.html").getByteArray();
		} else {
			Cout.debugSuperVisible("path: " + path);
			String context = path.remove(0);

			if (context.equals("api")) {
				return serveAPI(path, query);
			} else if (context.equals("file")) {
				return serveFiles(path, query);
			} else if (context.equals("idaweb")) {
				return serveIdaweb(path, query);
			} else if (context.equals("favicon.ico")) {
				return new JavaResource(RESTService.class, "flavicon.ico").getByteArray();
			} else {
				throw new IllegalStateException("unknown context: " + context);
			}
		}
	}

	private byte[] serveIdaweb(List<String> path, Map<String, String> query) throws Throwable {
		if (path.isEmpty()) {
			return new JavaResource(getClass(), "resources/index.html").getByteArray();
		} else {
			return new JavaResource(getClass(), "resources/" + TextUtilities.concatene(path, "/")).getByteArray();
		}
	}

	private byte[] serveFiles(List<String> path, Map<String, String> query) throws Throwable {
		return new RegularFile(Directory.getHomeDirectory(), TextUtilities.concatene(path, "/")).getContent();
	}

	private byte[] serveAPI(List<String> path, Map<String, String> query) throws Throwable {
		Serializer serializer = new GSONSerializer<>();

		if (query.containsKey("format")) {
			String format = query.get("format");
			serializer = name2serializer.get(format);

			if (serializer == null) {
				return ("unknow format: " + format + ". Available format are: " + name2serializer.keySet()).getBytes();
			}
		}

		Object result = processRESTRequest(path, query);

		if (result == null) {
			result = new NULL();
		}

		return serializer.toBytes(result);
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

	private Object processRESTRequest(List<String> path, Map<String, String> query) throws Throwable {
		double timeout = Double.valueOf(query.getOrDefault("timeout", "1"));

		if (path == null || path.isEmpty()) {
			return welcomePage();
		} else {
			Set<ComponentDescriptor> components = componentsFromURL(path.get(0));

			if (path.size() == 1) {
				return describeComponent(components, timeout).toArray(new ComponentDescriptor[0]);
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
						var stringParms = new OperationStringParameterList(
								path.size() == 3 ? new String[0] : path.get(3).split(","));
						System.out.println("calling operation " + components + "/" + serviceID.toString() + "/"
								+ operation + " with parameters: " + stringParms);
						List<Object> r = exec(new ServiceAddress(components, serviceID),
								new OperationID(serviceID, operation), true, stringParms).returnQ.setTimeout(timeout)
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

	private Set<ComponentDescriptor> componentsFromURL(String s) {
		if (s.isEmpty()) {
			return null;
		}

		Set<ComponentDescriptor> components = new HashSet<>();

		for (String name : s.split(",")) {
			var found = lookupService(RegistryService.class).lookup(name);

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
		var to = new ServiceAddress(components, ServiceManager.class);
		var res = exec(to, ServiceManager.list, true, null).returnQ;

		for (var m : res.setTimeout(timeout).collect().throwAnyError().resultMessages()) {
			ComponentDescriptor c = m.route.source().component;
			c.servicesNames = (Set<String>) m.content;
			r.add(c);
		}

		return r;
	}

	private Map<ComponentDescriptor, ServiceDescriptor> decribeService(Set<ComponentDescriptor> components,
			Class<? extends Service> serviceID) throws Throwable {
		Map<ComponentDescriptor, ServiceDescriptor> descriptors = new HashMap<>();
		var to = new ServiceAddress(components, serviceID);
		var res = exec(to, Service.descriptor, true, null).returnQ;

		for (Message m : res.collect().throwAnyError().resultMessages()) {
			descriptors.put(m.route.source().component, (ServiceDescriptor) m.content);
		}

		return descriptors;
	}

	public static class Welcome {
		String localComponent;
		Set<ComponentDescriptor> knownComponents = new HashSet<>();
	}

	private Welcome welcomePage() throws Throwable {
		Welcome w = new Welcome();
		w.localComponent = component.friendlyName;

		// asks all components to send their descriptor, which will be catched by the
		// networking service
		// that will pass it to the registry
		exec(new ServiceAddress((Set<ComponentDescriptor>) null, RegistryService.class), RegistryService.local, true,
				new OperationParameterList()).returnQ.setTimeout(1).collect();

		w.knownComponents.addAll(lookupService(RegistryService.class).list());

		var i = w.knownComponents.iterator();

		while (i.hasNext()) {
			var c = i.next();
			if (Math.random() < 0.5) {
				Cout.debug("remove " + c.friendlyName);
				i.remove();
			}
		}
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

	@IdawiOperation
	public void stopHTTPServer() throws IOException {
		if (restServer == null) {
			throw new IOException("REST server is not running");
		}

		restServer.stop(0);
		restServer = null;
	}

	@IdawiOperation
	private void startHTTPServerOperation(int port) throws IOException {
		startHTTPServer(port);
	}

}
