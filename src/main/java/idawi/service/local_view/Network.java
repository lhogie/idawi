package idawi.service.local_view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Stream;

import fr.cnrs.i3s.Cache;
import idawi.Component;
import idawi.RuntimeEngine;
import idawi.service.local_view.BFS.BFSResult;
import idawi.service.local_view.BFS.RRoute;
import idawi.service.local_view.BFS.Routes;
import idawi.transport.Link;
import idawi.transport.OutLinks;
import idawi.transport.TimeFrame;
import idawi.transport.Topologies;
import idawi.transport.TransportService;
import jdotgen.EdgeProps;
import jdotgen.GraphvizDriver;
import jdotgen.Props.Style;
import jdotgen.VertexProps;
import toools.io.file.RegularFile;

public class Network implements Serializable {
	public final Set<Component> components = new HashSet<>();
	public OutLinks links = new OutLinks();
	public final Map<Component, Cache<BFSResult>> src2bfsCache = new HashMap<>();
	public final List<NetworkTopologyListener> listeners = new ArrayList<>();

	public Component pickRandomPeer() {
		if (components.size() == 0) {
			return null;
		} else {
			var l = new ArrayList<>(components);
			return l.get(ThreadLocalRandom.current().nextInt(l.size()));
		}
	}

	public Stream<Link> snapshotAt(double time) {
		return links.stream().filter(l -> l.activity.availableAt(time));
	}

	public Stream<Link> inactiveLinks() {
		return links.set.stream().filter(l -> !l.isActive());
	}

	public RRoute findRoute(Component src, Component dest, int maxDistance) {
		return BFS.bfs(links, src, maxDistance, Integer.MAX_VALUE, c -> false, c -> false).predecessors.pathTo(dest);
	}

	public Routes findDisjointRoutes(Component src, Component dest, int maxDistance, Predicate<Routes> enough) {
		var routes = new Routes();
		routes.disjoint = true;
		var ignore = new HashSet<Component>();
		var bfs = BFS.bfs(links, src, maxDistance, Integer.MAX_VALUE, c -> false, c -> false);

		while (true) {
			var r = bfs.predecessors.pathTo(dest);

			if (r == null) {
				return routes;
			} else {
				routes.add(r);

				if (enough.test(routes)) {
					return routes;
				} else {
					r.asInfo().forEachComponent(c -> ignore.add(c));
				}
			}
		}
	}

	public void clear() {
		components.clear();
		links.clear();
	}

	public BFSResult bfs(Component from) {
		var cache = src2bfsCache.get(from);

		if (cache == null) {
			src2bfsCache.put(from, cache = new Cache<>(5,
					() -> BFS.bfs(links, from, 10, Integer.MAX_VALUE, avoid -> false, avoid -> false)));
		}

		return cache.get();
	}

	public void clearBFSCaches() {
		src2bfsCache.clear();
	}

	public void plot(RegularFile f) {
		f.setContent(Topologies.graphViz(components, links, c -> c.name()).toPDF());
	}

	public RegularFile plot() {
		var f = RegularFile.createTempFile("idawi-", ".pdf");
		f.setContent(Topologies.graphViz(components, links, c -> c.name()).toPDF());
		return f;
	}

	public Collection<Component> lookupByRegexp(String re) {
		return components.stream().filter(e -> e.matches(re)).toList();
	}

	public Link findLink(Component c) {
		return findLink(l -> l.dest.component.equals(c));
	}

	public Link findLink(Predicate<Link> p) {
		return links.stream().filter(p).findFirst().orElse(null);
	}

	public Link findLink(TransportService from, Component to) {
		return findLink(l -> l.src.equals(from) && l.dest.component.equals(to));
	}

	public Link findLink(TransportService from, TransportService to) {
		return findLink(l -> l.src.equals(from) && l.dest.equals(to));
	}

	public Link findReverseLink(Link l) {
		return findLink(l.dest, l.src.component);
	}

	public void activateLink(TransportService from, TransportService to) {
		var l = findLink(from, to);

		if (l == null) {
			links.add(l = new Link(from, to));
		}

		if (l.activity.isEmpty()) {
			l.activity.add(new TimeFrame(RuntimeEngine.now()));
		} else {
			var last = l.activity.last();

			if (last.isOver()) {
				l.activity.add(new TimeFrame(RuntimeEngine.now()));
			} else {
				last.end(RuntimeEngine.now());
			}
		}

		final var l_f = l;
		listeners.forEach(ll -> ll.linkActivated(l_f));
	}

	public void activateLink(TransportService from, TransportService to, boolean bidi) {
		activateLink(from, to);

		if (bidi) {
			activateLink(to, from);
		}
	}

	public void deactivateLink(TransportService from, TransportService to) {
		var l = findLink(from, to);

		if (l != null) {
			deactivateLink(l);
		}
	}

	public void deactivateLink(Link l) {
		links.remove(l);
		listeners.forEach(ll -> ll.linkDeactivated(l));
	}

	public GraphvizDriver toGraphvizDriver() {

		var w = new GraphvizDriver() {
			@Override
			protected void findVertices(VertexProps v, Validator f) {
				for (var c : components) {
					v.id = c.name();
					v.label = c.name();
					f.f();
				}
			}

			@Override
			protected void findEdges(EdgeProps v, Validator f) {
				for (var c : components) {
					for (var t : c.services(TransportService.class)) {
						for (var neighbor : t.outLinks().toList()) {
							v.from = t.component.name();
							v.to = neighbor.dest;
							v.label = t.getFriendlyName();
							v.directed = true;
							v.style = Style.dotted;
							f.f();
						}
					}
				}
			}
		};

		return w;
	}

	public Component lookup(String name) {
		return components.stream().filter(c -> c.name().matches(name)).findFirst().orElse(null);
	}

	public static void link(Component a, Component b, Class<? extends TransportService> t, boolean bidi) {
		link(a.need(t), b.need(t), bidi);
	}

	public static void link(TransportService a, TransportService b, boolean bidi) {
		Set.of(a, b).forEach(t -> t.component.localView().g.activateLink(a, b, bidi));
	}
}
