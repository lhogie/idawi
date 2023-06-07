package idawi.knowledge_base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import fr.cnrs.i3s.Cache;
import idawi.Component;
import idawi.TypedInnerClassOperation;
import idawi.knowledge_base.BFS.BFSResult;
import idawi.knowledge_base.BFS.RRoute;
import idawi.knowledge_base.BFS.Routes;
import idawi.knowledge_base.info.DirectedLink;
import idawi.knowledge_base.info.NeighborLost;
import idawi.routing.Route;
import idawi.transport.OutNeighbor;
import idawi.transport.TransportService;
import jdotgen.EdgeProps;
import jdotgen.GraphvizDriver;
import jdotgen.Props.Style;
import jdotgen.VertexProps;
import toools.io.Cout;
import toools.reflect.Clazz;

public class DigitalTwinService extends KnowledgeBase {

	public final Cache<BFSResult> bfsResult = new Cache<>(5,
			() -> BFS.bfs(component, 10, Integer.MAX_VALUE, avoid -> false));

	public final Set<Component> components = new HashSet<>();
	public final List<DigitalTwinListener> listeners = new ArrayList<>();
	public double minimumAccuracyTolerated = 0.1;

	public DigitalTwinService(Component component) {
		super(component);
		registerOperation(new addLink());
		registerOperation(new clear());
		registerOperation(new size());
		registerOperation(new components());
	}

	public TransportService twinTransport(TransportService t) {
		var twinComponent = localTwin(t.component);
		var twinTransport = twinComponent.lookup(t.getClass());

		if (twinTransport == null) {
			try {
				twinTransport = Clazz.makeInstance(t.getClass().getConstructor(), twinComponent);
			} catch (NoSuchMethodException | SecurityException err) {
				throw new RuntimeException(err);
			}
		}

		return twinTransport;
	}

	public void consider(NeighborLost l) {
		l.transport = twinTransport(l.transport);
		l.lostNeighbor = twinTransport(l.lostNeighbor);
		l.transport.neighborhood().remove(l.lostNeighbor);
	}

	public void consider(DirectedLink l) {
		l.transport = twinTransport(l.transport);
		l.dest.dest = twinTransport(l.dest.dest);
		l.transport.neighborhood().add(l.dest);
	}

	private static void removeOutdate(double now, Iterator<? extends Info> i) {
		while (i.hasNext()) {
			var n = i.next();

			if (n.reliability(now) < 0.1) {
				i.remove();
			}
		}
	}

	@Override
	public String getFriendlyName() {
		return "digital twin service";
	}

	public class addLink extends TypedInnerClassOperation {
		@Override
		public String getDescription() {
			return "accepts a new link info";
		}

		public void f(Collection<DirectedLink> newLinks) {
			newLinks.forEach(l -> add(l.date, l.transport, 0, l.dest.dest));
		}
	}

	public class size extends TypedInnerClassOperation {
		public int size() {
			return components.size();
		}

		@Override
		public String getDescription() {
			return "nb of components";
		}
	}

	public class components extends TypedInnerClassOperation {
		public Set<Component> components() {
			Cout.debug("running on " +  component +": " + components);
			return components;
		}

		@Override
		public String getDescription() {
			return "get all known components";
		}
	}

	public class clear extends TypedInnerClassOperation {
		public void f() {
			clear();
		}

		@Override
		public String getDescription() {
			return "clear the local view";
		}
	}

	public Set<Component> lookupByRegexp(String re) {
		var r = new HashSet<Component>();

		for (var e : components) {
			if (e.ref.matches(re)) {
				r.add(e);
			}
		}

		return r;
	}

	public Component localTwin(Component component) {
		for (var c : components) {
			if (c.equals(component)) {
				return c;
			}
		}

		add(component);
		return component;
	}

	@Override
	protected void forEachInfo(Predicate<Info> p) {
		components.forEach(c -> p.test(c.info));
	}

	public Component pickRandomPeer() {
		if (components.size() == 0) {
			return null;
		} else {
			var l = new ArrayList<>(components);
			return l.get(ThreadLocalRandom.current().nextInt(l.size()));
		}
	}

	public void feedWith(Route route) {
		var e = route.initialEmission;

		while (e != null) {
			var r = e.subsequentReception();
			add(component.now(), e.transport(), r.date() - e.date(), r.transport());
			e = r.forward();
		}
	}

	public void add(double now, TransportService from, double d, TransportService to) {
		add(from.component);
		add(to.component);
		var toNeighbor = from.neighborhood().search(to);

		if (toNeighbor == null) {
			from.neighborhood().add(toNeighbor = new OutNeighbor());
			toNeighbor.dest = to;
		}

		toNeighbor.duration = d;
		toNeighbor.date = component.now();
	}

	public Collection<Component> isolated() {
		return components.stream().filter(c -> searchLinkInvolving(c) == null).toList();
	}

	public void transports(Predicate<TransportService> p) {
		for (var c : components) {
			for (var s : c.services(TransportService.class)) {
				if (p.test(s)) {
					return;
				}
			}
		}
	}

	public void removeOutdated(double now) {
		for (var c : components) {
			for (var t : c.services(TransportService.class)) {
				var neighbors = t.neighborhood().infos().iterator();

				while (neighbors.hasNext()) {
					var n = neighbors.next();

					if (n.reliability(now) < minimumAccuracyTolerated) {
						neighbors.remove();
					}
				}
			}
		}
	}

	public GraphvizDriver toGraphvizDriver() {

		var w = new GraphvizDriver() {
			@Override
			protected void findVertices(VertexProps v, Validator f) {
				for (var c : components) {
					v.id = c.ref;
					v.label = c.ref;
					f.f();
				}
			}

			@Override
			protected void findEdges(EdgeProps v, Validator f) {
				for (var c : components) {
					for (var t : c.services(TransportService.class)) {
						for (var neighbor : t.neighborhood().infos()) {
							v.from = t.component.ref;
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

	public RRoute findRoute(Component src, Component dest, int maxDistance) {
		return BFS.bfs(src, maxDistance, Integer.MAX_VALUE, c -> false).predecessors.path(src, dest);
	}

	public Routes findRoutes(Component src, Component dest, int maxDistance, Predicate<Routes> enough) {
		var routes = new Routes();
		var ignore = new HashSet<Component>();

		while (true) {
			var r = BFS.bfs(src, maxDistance, Integer.MAX_VALUE, c -> false).predecessors.path(src, dest);

			if (r == null) {
				return routes;
			} else {
				routes.add(r);

				if (enough.test(routes)) {
					return routes;
				} else {
					r.forEachComponent(c -> ignore.add(c));
				}
			}
		}
	}

	public void searchEdge(Component src, Class<? extends TransportService> protocol, Component dest,
			Predicate<TransportService> stop) {
		transports(t -> matches(t, src, protocol, dest) && stop.test(t));
	}

	private boolean matches(TransportService t, Component src, Class<? extends TransportService> protocol,
			Component dest) {
		return (src == null || t.component.equals(src)) && (protocol == null || t.getClass() == protocol)
				&& (dest == null || t.neighborhood().contains(dest));
	}

	public TransportService searchLinkInvolving(Component a) {
		if (!components.contains(a)) {
			return null;
		}

		for (var c : components) {
			for (var t : c.services(TransportService.class)) {
				if (t.component.equals(a) || t.actualNeighbors().contains(a)) {
					return t;
				}
			}
		}

		return null;
	}

	public boolean contains(Component a) {
		return components.contains(a);
	}

	public boolean add(Component a) {
		if (components.contains(a)) {
			return false;
		}

		components.add(a);
		listeners.forEach(l -> l.newComponent(a));
		return true;
	}

	public void clear() {
		components.clear();
	}

	public Component lookup(String name) {
		for (var c : components) {
			if (c.ref.equals(name)) {
				return c;
			}
		}

		return null;
	}

}
