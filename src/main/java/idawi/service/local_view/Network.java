package idawi.service.local_view;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import fr.cnrs.i3s.Cache;
import idawi.Component;
import idawi.IdawiGraphvizDriver;
import idawi.RuntimeEngine;
import idawi.Stop;
import idawi.service.local_view.BFS.BFSResult;
import idawi.service.local_view.BFS.RRoute;
import idawi.service.local_view.BFS.Routes;
import idawi.transport.Link;
import idawi.transport.TransportService;
import jdotgen.GraphvizDriver;
import jdotgen.GraphvizDriver.OUTPUT_FORMAT;
import toools.SizeOf;
import toools.collections.Collections;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class Network implements Serializable, SizeOf {
	public class BFSCache extends HashMap<Component, Cache<BFSResult>> {
		public BFSResult from(Component from) {
			var cache = get(from);

			if (cache == null) {
				put(from, cache = new Cache<>(5,
						() -> BFS.bfs(Network.this, from, 10, Integer.MAX_VALUE, avoid -> false, avoid -> false)));
			}

			return cache.get();
		}
	}

	private final List<Component> components = new ArrayList<>();
	private Collection<Link> links = new ArrayList<>();

	public final BFSCache bfs = new BFSCache();
	public final List<NetworkTopologyListener> listeners = new ArrayList<>();

	public synchronized void add(Component twin) {
		// if (!twin.isDigitalTwin())
		// throw new IllegalStateException();

		components.add(twin);
		listeners.forEach(l -> l.newComponent(twin));
	}

	public synchronized void clear() {
		components.clear();
		links.clear();
		bfs.clear();
	}

	@Override
	public synchronized long sizeOf() {
		return SizeOf.sizeOf(components) + SizeOf.sizeOf(links);
	}

	public synchronized Link findALink(Predicate<Link> p) {
		for (var l : links) {
			if (p.test(l)) {
				return l;
			}
		}

		return null;
	}

	public synchronized Component findAComponent(Predicate<Component> p) {
		for (var l : components) {
			if (p.test(l)) {
				return l;
			}
		}

		return null;
	}

	public synchronized Link ensureExists(Link l) {
		var r = findALinkLike(l);

		if (r == null) {
			if (findComponentLike(l.src.component) == null) {
				add(l.src.component);
			}

			if (findComponentLike(l.dest.component) == null) {
				add(l.dest.component);
			}

			links.add(r = l);
		}

		return r;
	}

	public synchronized int nbLinks() {
		return links.size();
	}

	public synchronized Component pickRandomComponent(Random r) {
		if (components.size() == 0) {
			return null;
		} else {
			return components.get(r.nextInt(components.size()));
		}
	}

	public synchronized Link randomLinks(Random prng) {
		return Collections.pickRandomObject(links, prng);
	}

	public synchronized Collection<Link> randomLinks(int n,Random r) {
		if (links.size() <= n) {
			return new HashSet<>(links);
		} else {
			return Collections.pickRandomSubset(links, n, false, r);
		}
	}

	public synchronized List<Component> lookupByRegexp(String re) {
		return findComponents(c -> c.name().matches(re));
	}

	public synchronized Link forEachLink(Function<Link, Stop> f) {
		for (var l : links) {
			if (f.apply(l) == Stop.yes) {
				return l;
			}
		}

		return null;
	}

	public synchronized void forEachComponent(Function<Component, Stop> f) {
		for (var c : components) {
			if (f.apply(c) == Stop.yes) {
				break;
			}
		}
	}

	public List<Link> snapshotAt(double time) {
		return findLinks(l -> l.activity.availableAt(time));
	}

	public Link findALinkLike(Link link) {
		return findALink(l -> l.equals(link));
	}

	public List<Link> findInactiveLinks() {
		return findLinks(l -> !l.isActive());
	}

	public Component findComponent(String name) {
		var a = new Holder<Component>();

		forEachComponent(c -> {
			if (c.name().equals(name)) {
				a.c = c;
				return Stop.yes;
			} else {
				return Stop.no;
			}
		});

		return a.c;
	}

	public Component findComponentLike(Component c) {
		return findComponent(c.name());
	}

	public List<Link> findLinksTo(Component c) {
		return findLinks(l -> l.dest.component.equals(c));
	}

	public List<Link> findLinksTo(TransportService c) {
		return findLinks(l -> l.dest.equals(c));
	}

	public List<Link> findLinksConnecting(Component from, Component to) {
		return findLinks(l -> l.src.component.equals(from) && l.dest.component.equals(to));
	}

	public Link findALinkConnecting(TransportService from, TransportService to) {
		return findALink(l -> l.src.equals(from) && l.dest.equals(to));
	}

	public Link findALinkConnecting(TransportService from, Component to) {
		return findALink(l -> l.src.equals(from) && l.dest.component.equals(to));
	}

	@Override
	public String toString() {
		var sw = new StringWriter();
		var s = new PrintWriter(sw);

		forEachComponent(c -> {
			s.println(c);
			return Stop.no;
		});
		
		forEachLink(l -> {
			s.println(l.src + " -> " + l.dest);
			return Stop.no;
		});
		
		return sw.toString();
	}

	public Link findALinkConnecting(Component component, Component relay) {
		return findALink(l -> l.src.component.equals(component) && l.dest.component.equals(relay));
	}

	public RRoute findRoute(Component src, Component dest, int maxDistance) {
		return BFS.bfs(this, src, maxDistance, Integer.MAX_VALUE, c -> false, c -> false).predecessors.pathTo(dest);
	}

	public Routes findDisjointRoutes(Component src, Component dest, int maxDistance, Predicate<Routes> enough) {
		var routes = new Routes();
		routes.disjoint = true;
		var ignore = new HashSet<Component>();
		var bfs = BFS.bfs(this, src, maxDistance, Integer.MAX_VALUE, c -> false, c -> false);

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

	static class Holder<E> {
		public E c;
	}

	public Link markLinkActive(TransportService src, TransportService dest) {
		return markLinkActive(new Link(src, dest));
	}

	public Link markLinkActive(Link l) {
		l = ensureExists(l);

		if (!l.isActive()) {
			l.markActive();
			var finalL = l;
			listeners.forEach(listener -> listener.linkActivated(finalL));
		}

		return l;
	}

	public static void markLinkActive(TransportService src, TransportService dest, boolean doOtherDirection,
			Collection<Component> whoToinform) {
		if (whoToinform.isEmpty())
			throw new IllegalStateException();

		whoToinform.forEach(c -> c.localView().g.markLinkActive(src, dest));

		if (doOtherDirection) {
			markLinkActive(dest, src, false, whoToinform);
		}
	}

	public static void markLinkActive(Component a, Component b, Class<? extends TransportService> t,
			boolean doBothDirections, Collection<Component> whoToinform) {
		markLinkActive(a.service(t, true), b.service(t, true), doBothDirections, whoToinform);
	}

	public static void deactivateLink(Link l, boolean doOtherDirection, Collection<Component> whoToinform) {
		whoToinform.forEach(t -> t.localView().g.deactivateLink(l));

		if (doOtherDirection) {
			deactivateLink(new Link(l.dest, l.src), false, whoToinform);
		}
	}

	public void deactivateLink(TransportService from, TransportService to) {
		var l = findALinkConnecting(from, to);

		if (l != null) {
			deactivateLink(l);
		}
	}

	public void deactivateLink(TransportService from, TransportService to, boolean bidi) {
		deactivateLink(from, to);

		if (bidi) {
			deactivateLink(to, from);
		}
	}

	public void deactivateLink(Link l) {
		l.markInactive();
//			links.remove(l);
		listeners.forEach(ll -> ll.linkDeactivated(l));
	}

	public Directory plot() {
		return plot("", custom -> {
		});
	}

	public Directory plot(Object label, Consumer<IdawiGraphvizDriver> customizer) {
		var filename = String.format("%03f", RuntimeEngine.now()) + " " + label;
		var d = new IdawiGraphvizDriver(this);
		customizer.accept(d);
		var dot = d.toDot();

		d.outputFormats.forEach(ext -> {
			var bytes = GraphvizDriver.to(d.cfg, dot, OUTPUT_FORMAT.valueOf(ext));
			new RegularFile(RuntimeEngine.directory, filename + "." + ext).setContent(bytes);
		});

		return RuntimeEngine.directory;
	}

	public List<Link> findLinks(Predicate<Link> p) {
		var r = new ArrayList<Link>();

		forEachLink(l -> {
			if (p.test(l)) {
				r.add(l);
			}

			return Stop.no;
		});

		return r;
	}

	public List<Component> findComponents(Predicate<Component> p) {
		var r = new ArrayList<Component>();

		forEachComponent(c -> {
			if (p.test(c)) {
				r.add(c);
			}

			return Stop.no;
		});

		return r;
	}

	public List<Component> components() {
		return findComponents(c -> true);
	}

	public List<Link> links() {
		return findLinks(l -> true);
	}

}
