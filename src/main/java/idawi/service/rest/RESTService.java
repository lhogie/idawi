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
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Message;
import idawi.MessageException;
import idawi.Operation;
import idawi.OperationDescriptor;
import idawi.Service;
import idawi.net.JacksonSerializer;
import idawi.net.LMI;
import idawi.net.NetworkingService;
import idawi.service.PingPong;
import idawi.service.ServiceManager;
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
			path = new ArrayList<>();
			path.add("antonin");
		}

		String context = path.remove(0);

		if (context.equals("api")) {
			processAPI(path.subList(1, path.size()), query, out);
		} else if (context.equals("antonin")) {
			processHTML(path, query, out);
		}
	}

	private void processHTML(List<String> path, Map<String, String> query, Consumer<byte[]> out) throws Throwable {
//		String html = new String(new JavaResource(getClass(), "index.html").getByteArray());

		var f = new RegularFile("$HOME/idawi/" + TextUtilities.concatene(path, "/"));
		System.out.println("reading " + f);
		out.accept(f.getContentAsText().getBytes());
	}

	private void processAPI(List<String> path, Map<String, String> query, Consumer<byte[]> out) throws Throwable {
		String format = query.get("format");
		var serializer = name2serializer.getOrDefault(format, new GSONSerializer<>());

		if (serializer == null) {
			out.accept(("unknow format: " + format + ". Available format are: " + name2serializer.keySet()).getBytes());
		} else {
			try {
				Object result = processRESTRequest(path, query);
				byte[] json = serializer.toBytes(result);
				out.accept(json);
			} catch (Throwable t) {
				out.accept(serializer.toBytes(t));
				throw t;
			}
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

	private Object processRESTRequest(List<String> path, Map<String, String> query) throws MessageException {
		double timeout = Double.valueOf(query.getOrDefault("timeout", "1"));

		if (path == null || path.isEmpty()) {
			return welcomePage();
		} else {
			Set<ComponentInfo> components = componentsFromURL(path.get(0));

			if (path.size() == 1) {
				return describeComponent(components, timeout);
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
						throw new Error("path too long! Expecting: component/service/action");
					} else {
						var stringParms = path.size() == 3 ? new String[0] : path.get(3).split(",");
						System.out.println("calling action " + components + "/" + serviceID.toString() + "/" + operation
								+ "(" + Arrays.toString(stringParms) + ")");
						return call(components, serviceID, operation, stringParms).setTimeout(timeout).collect()
								.resultMessages().contents();
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
			Set<ComponentInfo> found = component.descriptorRegistry.lookupByName(name);

			if (components.isEmpty()) {
				components.add(ComponentInfo.fromCDL("name=" + name));
			} else {
				components.addAll(found);
			}
		}

		return components;
	}

	private Set<ComponentInfo> describeComponent(Set<ComponentInfo> components, double timeout) {
		Set<ComponentInfo> r = new HashSet<>();

		for (var m : call(components, ServiceManager.class, "list").setTimeout(timeout).collect().resultMessages()) {
			ComponentInfo c = m.route.source().component;
			c.servicesStrings = (Set<String>) m.content;
			r.add(c);
		}

		return r;
	}

	private Map<String, Set<OperationDescriptor>> decribeService(Set<ComponentInfo> components,
			Class<? extends Service> serviceID) throws MessageException {
		Map<String, Set<OperationDescriptor>> component2serviceList = new HashMap<>();

		for (Message m : call(components, serviceID, Service.listOperationNames).collect().throwAnyError()
				.resultMessages()) {
			component2serviceList.put(m.route.source().component.friendlyName + "/" + serviceID,
					(Set<OperationDescriptor>) m.content);
		}

		return component2serviceList;
	}

	public static class Welcome {
		String localComponent;
		Set<String> knownComponents = new HashSet<>();
	}

	private Welcome welcomePage() {
		Welcome w = new Welcome();
		w.localComponent = component.descriptor().friendlyName;

		component.lookupService(PingPong.class).discover(1, found -> w.knownComponents.add(found.friendlyName));

		for (var c : component.descriptorRegistry) {
			w.knownComponents.add(c.friendlyName);
		}

		w.knownComponents.add(component.descriptor().friendlyName);

		for (ComponentInfo d : component.lookupService(NetworkingService.class).neighbors()) {
			w.knownComponents.add(d.friendlyName);
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

	@Operation
	private void stoptRestServer(Message msg, Consumer<Object> returns) throws IOException {
		stopHTTPServer();
	}

	@Operation
	private Object listServices(HttpExchange e) {
		return component.services().stream().map(s -> s.getFriendlyName()).collect(Collectors.toSet());
	}

	@Operation
	private Object printRequest(HttpExchange e) {
		return e;
	}

	public static void main(String[] args) throws IOException, MessageException {
		List<Component> components = new ArrayList();

		for (int i = 0; i < 20; ++i) {
			components.add(new Component());
		}

		LMI.chain(components);
		components.get(0).lookupService(RESTService.class).startHTTPServer();

		Threads.sleepForever();
	}
}
