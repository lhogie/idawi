package idawi.service.local_view;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import fr.cnrs.i3s.Cache;
import idawi.Component;
import idawi.Idawi;
import idawi.IdawiGraphvizDriver;
import idawi.service.local_view.BFS.BFSResult;
import idawi.service.local_view.BFS.RRoute;
import idawi.service.local_view.BFS.RouteList;
import idawi.transport.Link;
import idawi.transport.TransportService;
import jdotgen.GraphvizDriver;
import jdotgen.GraphvizDriver.OUTPUT_FORMAT;
import toools.Stop;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class Network extends ThreadSafeNetworkDataStructure {
	public class BFSCache extends HashMap<Component, Cache<BFSResult>> {
		public BFSResult from(Component from) {
			var cache = get(from);

			if (cache == null) {
				put(from, cache = new Cache<>(5, () -> BFS.bfs(Network.this, from, 10, Integer.MAX_VALUE,
						ignoreComponent -> false, ignoreLink -> false)));
			}

			return cache.get();
		}
	}

	public final BFSCache bfs = new BFSCache();

	@Override
	public synchronized void clear() {
		super.clear();
		bfs.clear();
	}

	public List<Component> lookupByRegexp(String re) {
		return findComponents(c -> c.friendlyName.matches(re));
	}

	public List<Link> snapshotAt(double time) {
		return findLinks(l -> l.activity.availableAt(time));
	}

	public List<Link> findInactiveLinks() {
		return findLinks(l -> !l.isActive());
	}

	public Component findComponentByPublicKey(PublicKey k, boolean autoCreate) {
		return findComponent(c -> c.publicKey() != null && c.publicKey().equals(k), autoCreate, null);
	}

	public Component findComponentByFriendlyName(String name, boolean autoCreate) {
		return findComponent(c -> c.friendlyName != null && c.friendlyName.equals(name), autoCreate, null);
	}

	public List<Link> findLinksTo(Component c) {
		return findLinks(l -> l.dest.component.equals(c));
	}

	public List<Link> findLinksTo(TransportService c) {
		return findLinks(l -> l.dest.equals(c));
	}

	public List<Link> findLinksFrom(TransportService c) {
		return findLinks(l -> l.src.equals(c));
	}

	public List<Link> findLinksConnecting(Component from, Component to) {
		return findLinks(l -> l.src.component.equals(from) && l.dest.component.equals(to));
	}

	public Link findALinkConnecting(TransportService from, TransportService to) {
		return findLink(l -> l.src.equals(from) && l.dest.equals(to));
	}

	public Link findALinkConnecting(TransportService from, Component to) {
		return findLink(l -> l.src.equals(from) && l.dest.component.equals(to));
	}

	@Override
	public String toString() {
		var sw = new StringWriter();
		var s = new PrintWriter(sw);

		forEachComponent(c -> {
			s.println(c);
			return Stop.no;
		});

		final  Set<Link> ignore = new HashSet<>();
		

		forEachLink(l -> {
			if (ignore.contains(l))
				return Stop.no;
			
			var reverseLink = findALinkConnecting(l.dest, l.src);	
			
			if (reverseLink != null) {
				s.println(l.src + " -- " + l.dest);
				ignore.add(reverseLink);
			}else {
				s.println(l.src + " -> " + l.dest);
			}
			
			return Stop.no;
		});

		return sw.toString();
	}

	public Link findALinkConnecting(Component component, Component relay) {
		return findLink(l -> l.src.component.equals(component) && l.dest.component.equals(relay));
	}

	public RRoute findRoute(Component src, Component dest, int maxDistance) {
		return BFS.bfs(this, src, maxDistance, Integer.MAX_VALUE, c -> false, c -> false).predecessors.pathTo(dest);
	}

	public RouteList findDisjointRoutes(Component src, Component dest, int maxDistance, Predicate<RouteList> enough) {
		var routes = new RouteList();
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

	public void markLinkActive(TransportService src, TransportService dest, boolean bothDirections) {
		markLinkActive(src, dest);

		if (bothDirections) {
			markLinkActive(dest, src);
		}
	}

	public void markLinkActive(Component src, Component dest, Class<? extends TransportService> t,
			boolean bothDirections) {

		markLinkActive(src.service(t, true), dest.service(t, true), bothDirections);
	}

	public Link markLinkActive(TransportService src, TransportService dest) {
		var l = findLink(src, dest, true, null);

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
		var filename = String.format("%03f", Idawi.agenda.time()) + " " + label;
		var d = new IdawiGraphvizDriver(this);
		customizer.accept(d);
		var dot = d.toDot();

		d.outputFormats.forEach(ext -> {
			var bytes = GraphvizDriver.to(d.cfg, dot, OUTPUT_FORMAT.valueOf(ext));
			new RegularFile(Idawi.directory, filename + "." + ext).setContent(bytes);
		});

		return Idawi.directory;
	}

	public byte[] toPDF() {
		var d = new IdawiGraphvizDriver(this);
		return GraphvizDriver.to(d.cfg, d.toDot(), OUTPUT_FORMAT.pdf);
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
