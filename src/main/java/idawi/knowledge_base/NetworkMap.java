package idawi.knowledge_base;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import idawi.Component;
import idawi.knowledge_base.info.DirectedLink;
import idawi.knowledge_base.info.NeighborLost;
import idawi.routing.Emission;
import idawi.routing.Reception;
import idawi.routing.Route;
import idawi.transport.TransportService;
import toools.exceptions.NotYetImplementedException;

public class NetworkMap implements Serializable {
	public final Set<ComponentRef> components = new HashSet<>();
	public final List<DirectedLink> links = new ArrayList<>();
	public final List<NetworkTopologyListener> listeners = new ArrayList<>();
	public double minimumAccuracyTolerated = 0.1;

	public ComponentRef pickRandomPeer() {
		if (components.size() == 0) {
			return null;
		} else {
			var l = new ArrayList<>(components);
			return l.get(ThreadLocalRandom.current().nextInt(l.size()));
		}
	}

	public void feedWith(Route route) {
		Emission e = route.initialEmission();

		while (e != null && e.hasNext()) {
			Reception r = e.next();
			add(new DirectedLink(e.date(), e.component, e.transport(), r.component));
			e = r.forward();
		}
	}

	public Collection<ComponentRef> outNeighbors(ComponentRef c) {
		return outs(c).stream().map(l -> l.dest).toList();
	}

	public Collection<DirectedLink> outs(ComponentRef p) {
		return links.stream().filter(l -> l.src.equals(p)).toList();
	}

	public Collection<ComponentRef> isolated() {
		return components.stream().filter(c -> searchLinkInvolving(c) == null).toList();
	}

	public Collection<DirectedLink> ins(ComponentRef p) {
		return links.stream().filter(l -> l.dest.equals(p)).toList();
	}

	public void removeOutdated(double now) {
		links.removeIf(e -> e.reliability(now) < minimumAccuracyTolerated);
	}

	public Collection<ComponentRef> sources() {
		return links.stream().map(l -> l.src).toList();
	}

	public List<ComponentRef> findRoute(ComponentRef src, ComponentRef dest, int maxDistance) {
		return BFS.bfs(this, src, maxDistance, Integer.MAX_VALUE).predecessors.path(src, dest);
	}

	public DirectedLink searchEdge(ComponentRef src, Class<? extends TransportService> protocol, ComponentRef dest) {
		class A {
			DirectedLink l;
		}
		A a = new A();
		List<DirectedLink> r = new ArrayList<>();
		searchEdge(src, protocol, dest, l -> {
			a.l = l;
			return true;
		});

		return a.l;
	}

	public List<DirectedLink> searchEdges(ComponentRef src, Class<? extends TransportService> protocol,
			ComponentRef dest, Predicate<DirectedLink> stop) {
		List<DirectedLink> r = new ArrayList<>();
		searchEdge(src, protocol, dest, l -> {
			r.add(l);
			return stop.test(l);
		});

		return r;
	}

	public void searchEdge(ComponentRef src, Class<? extends TransportService> protocol, ComponentRef dest,
			Predicate<DirectedLink> stop) {
		for (var e : links) {
			if (e.matches(src, protocol, dest)) {
				if (stop.test(e)) {
					return;
				}
			}
		}
	}

	public DirectedLink searchLinkInvolving(ComponentRef a) {
		for (var e : links) {
			if (e.src.equals(a) || e.dest.equals(a)) {
				return e;
			}
		}

		return null;
	}

	public boolean contains(ComponentRef a) {
		return components.contains(a);
	}

	public boolean add(ComponentRef a) {
		if (components.contains(a)) {
			return false;
		}

		components.add(a);
		listeners.forEach(l -> l.newComponent(a));
		return true;
	}

	public boolean remove(ComponentRef a) {
		if (components.contains(a)) {
			// remove all links involving the component
			remove(a, null, null);
			remove(null, null, a);

			components.remove(a);
			listeners.forEach(l -> l.componentHasGone(a));
			return true;
		}

		return false;
	}

	public void consider(DirectedLink ke) {
		searchLinkInvolving(null);
	}

	public void consider(NeighborLost ke) {
		throw new NotYetImplementedException();
	}

	public void remove(ComponentRef src, Class<? extends TransportService> p, ComponentRef dest) {
		var i = links.iterator();

		while (i.hasNext()) {
			var e = i.next();

			if (e.matches(src, p, dest)) {
				i.remove();
				listeners.forEach(l -> l.interactionStopped(e));
			}
		}
	}

	public List<DirectedLink> edges() {
		return links;
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
//		links.forEach(l -> out.println(l.src + " -> " + l.dest + " [label=\"" + l.transport.getName() + "\"];"));
		links.forEach(l -> out.println(l.src + " -> " + l.dest + ";"));
		out.println("}");
	}

	public Collection<ComponentRef> neighbors(ComponentRef c, Class<? extends TransportService> transport) {
		Collection<ComponentRef> peers = new HashSet<>();
		outs(c).forEach(e -> {
			if (e.transport == transport) {
				peers.add(e.dest);
			}
		});
		return peers;
	}

	public void clear() {
		links.clear();
	}

	public void add(DirectedLink l) {
		var a = searchEdge(l.src, l.transport, l.dest);

		if (a == null) {
			links.add(l);
			add(l.src);
			add(l.dest);
			listeners.forEach(listener -> listener.newInteraction(l));
		} else if (l.isNewerThan(a)) {
			a.date = l.date;
		}
	}

	public ComponentRef lookup(String n) {
		for (var r : components) {
			if (r.ref.equals(n)) {
				return r;
			}
		}

		return null;
	}


}
