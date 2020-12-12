package idawi.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Message;
import idawi.MessageException;
import idawi.Operation;
import idawi.Service;
import idawi.net.JacksonSerializer;
import idawi.net.NetworkingService;
import toools.io.ser.FSTSerializer;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;
import toools.reflect.Clazz;
import toools.thread.Threads;

public class RESTService extends Service {
	public static class XMLSerializer<E> extends Serializer<E> {
		@Override
		public E read(InputStream is) throws IOException {
			XmlMapper xmlMapper = new XmlMapper();
			throw new IllegalStateException();
		}

		@Override
		public void write(E o, OutputStream out) throws IOException {
			XmlMapper xmlMapper = new XmlMapper();
			xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
			xmlMapper.writeValue(out, o);
		}

		@Override
		public String getMIMEType() {
			return "XML";
		}
	}

	private HttpServer restServer;
	public Map<String, Serializer> name2serializer = new HashMap<>();

	public RESTService(Component t) {
		super(t);
		name2serializer.put("JSON", new GSONSerializer<>());
		name2serializer.put("JSON2", new JacksonSerializer());
		name2serializer.put("SER", new JavaSerializer<>());
		name2serializer.put("FST", new FSTSerializer<>());
		name2serializer.put("XML", new XMLSerializer<>());
	}

	public HttpServer startHTTPServer() throws IOException {
		return startHTTPServer(8080);
	}

	public HttpServer startHTTPServer(int port) throws IOException {
		if (restServer != null) {
			throw new IOException("REST server is already running");
		}

		restServer = HttpServer.create(new InetSocketAddress("localhost", port), 0);
		restServer.createContext("/", e -> requestHandler(e));
		restServer.setExecutor(Service.threadPool);
		restServer.start();
		return restServer;
	}

	private void requestHandler(HttpExchange e) {
		URI uri = e.getRequestURI();
		Map<String, String> query = query(uri.getQuery());
		String format = query.getOrDefault("format", "JSON");
		var serializer = name2serializer.get(format);

		if (serializer == null) {
			throw new Error("unknow format: " + format + ". Available format are: " + name2serializer.keySet());
		}

		try {
			String[] path = path(uri.getPath());
			Object result = processRESTRequest(path, query);
			byte[] json = serializer.toBytes(result);
			sendBack(json, e);
		} catch (Throwable t) {
			t.printStackTrace();
			sendBack(serializer.toBytes(t), e);
		}
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

	private String[] path(String s) {
		if (s == null) {
			return null;
		}

		if (s.startsWith("/")) {
			s = s.substring(1);
		}

		if (s.endsWith("/")) {
			s = s.substring(0, s.length() - 1);
		}

		return s.isEmpty() ? null : s.split("/");
	}

	private Object processRESTRequest(String[] path, Map<String, String> query) throws MessageException {
		double timeout = Double.valueOf(query.getOrDefault("timeout", "1"));

		if (path == null || path.length == 0) {
			return welcomePage();
		} else {
			Set<ComponentInfo> components = componentsFromURL(path[0]);

			if (path.length == 1) {
				return describeComponent(components, timeout);
			} else {
				String serviceName = path[1];
				Class<? extends Service> serviceID = Clazz.findClass(serviceName);

				if (serviceID == null) {
					throw new Error("service " + serviceName + " is not known");
				} else if (path.length == 2) {
					return decribeService(components, serviceID);
				} else {
					String operation = path[2];

					if (path.length > 4) {
						throw new Error("path too long! Expecting: component/service/action");
					} else {
						var stringParms = path.length == 3 ? new String[0] : path[3].split(",");
						System.out.println("invoking action " + operation + " service " + components + "/"
								+ serviceID.getClass().toString() + " with parameters: " + stringParms);
						return call(components, serviceID, "callRESTOperation", operation, stringParms)
								.setTimeout(timeout).collect().contents();
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
				components.add(ComponentInfo.fromPDL("name=" + name));
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

	private Map<String, Set<String>> decribeService(Set<ComponentInfo> components, Class<? extends Service> serviceID)
			throws MessageException {
		Map<String, Set<String>> component2serviceList = new HashMap<>();

		for (Message m : call(components, serviceID, "listNativeActions").collect().throwAnyError().resultMessages()) {
			component2serviceList.put(m.route.source().component.friendlyName + "/" + serviceID,
					(Set<String>) m.content);
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
		Component t = new Component();
		t.lookupService(RESTService.class).startHTTPServer();

		Set<Component> components = t.lookupService(ComponentDeployer.class).deployLocalPeers(1, true, peerOk -> {
			System.out.println("new component: " + peerOk);
		});

		Threads.sleepForever();
	}
}
