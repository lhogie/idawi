package idawi.map;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import idawi.Component;
import idawi.ComponentDescriptor;
import toools.thread.Threads;
import toools.util.Date;

public class NetworkMap {
	static {
		Threads.newThread_loop(1000, () -> true, () -> {
			Component.componentsInThisJVM.values().forEach(c -> c.lookupService(NetworkMap.class).removeOutdated());
		});
	}

	public static class Edge {
		public String protocolName;
		public ComponentDescriptor c;
		public double creationDate = Date.time();
		public static double validity = 10;

		public Edge(String protocol, ComponentDescriptor dest) {
			this.protocolName = protocol;
			this.c = dest;
		}

		public boolean expired() {
			return Date.time() > creationDate + validity;
		}

		public double pertinence() {
			double age = Date.time() - creationDate;
			double ageRatio = age / validity;
			return 1 - ageRatio;
		}
	}

	private final Map<ComponentDescriptor, Set<Edge>> m = new HashMap<>();
	public final List<NetworkMapListener> listener = new ArrayList<>();

	public Set<Edge> get(ComponentDescriptor p) {
		return m.get(p);
	}

	private void removeOutdated() {
		m.values().forEach(s -> s.removeIf(e -> e.expired()));
	}

	public Set<ComponentDescriptor> peers() {
		return m.keySet();
	}

	public void add(ComponentDescriptor p) {
		m.put(p, new HashSet<>());
		listener.forEach(l -> l.newNode(p));
	}

	public void add(ComponentDescriptor a, String protocolName, ComponentDescriptor b) {
		if (!contains(a)) {
			add(a);
		}

		if (!contains(b)) {
			add(b);
		}

		if (searchEdge(a, protocolName, b) == null) {
			m.get(a).add(new Edge(protocolName, b));
			m.get(b).add(new Edge(protocolName, a));
			listener.forEach(l -> l.newEdge(a, b));
		}
	}

	public Edge searchEdge(ComponentDescriptor src, ComponentDescriptor dest) {
		for (var e : get(src)) {
			if (e.c.equals(dest)) {
				return e;
			}
		}

		return null;
	}

	public Edge searchEdge(ComponentDescriptor src, String protocol, ComponentDescriptor dest) {
		for (var e : get(src)) {
			if (e.protocolName.equals(protocol) && e.c.equals(dest)) {
				return e;
			}
		}

		return null;
	}

	public boolean contains(ComponentDescriptor a) {
		return m.containsKey(a);
	}

	public void remove(ComponentDescriptor src, String p, ComponentDescriptor dest) {
		Edge e = searchEdge(src, dest);

		if (e != null) {
			m.remove(e);
			listener.forEach(l -> l.edgeRemoved(src, dest));
		}
	}

	public void removeBidi(ComponentDescriptor a, String p, ComponentDescriptor b) {
		remove(a, p, b);
		remove(b, p, a);
	}

	public void forEachEdge(BiConsumer<ComponentDescriptor, Edge> c) {
		for (ComponentDescriptor u : m.keySet()) {
			for (Edge e : m.get(u)) {
				c.accept(u, e);
			}
		}
	}

	public String toDot() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		toDot(pw);
		pw.flush();
		return sw.toString();
	}

	public void toDot(PrintWriter out) {
		out.println("digraph {");
		forEachEdge((src, e) -> out.println(src.friendlyName + " --" + e.protocolName + "--> " + e.c + ";"));
		out.println("}");
	}
}
