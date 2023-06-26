package idawi.service.local_view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Stream;

import fr.cnrs.i3s.Cache;
import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.routing.RouteListener;
import idawi.routing.Route;
import idawi.service.DigitalTwinService;
import idawi.service.digital_twin.info.KNW_NeighborLost;
import idawi.service.local_view.BFS.BFSResult;
import idawi.service.local_view.BFS.RRoute;
import idawi.service.local_view.BFS.Routes;
import idawi.transport.Link;
import idawi.transport.TransportService;
import jdotgen.EdgeProps;
import jdotgen.GraphvizDriver;
import jdotgen.Props.Style;
import jdotgen.VertexProps;
import toools.io.Cout;

public class LocalViewService extends KnowledgeBase implements RouteListener{

	public final Cache<BFSResult> bfsResult = new Cache<>(5,
			() -> BFS.bfs(component, 10, Integer.MAX_VALUE, avoid -> false));

	private final Set<Component> components = new HashSet<>();
	public final List<DigitalTwinListener> listeners = new ArrayList<>();
	public double minimumAccuracyTolerated = 0.1;

	private List<Link> links = new ArrayList<>();

	public LocalViewService(Component component) {
		super(component);

		// the component won't have twin
		components.add(component);
	}

	@Override
	public String getFriendlyName() {
		return "digital twin service";
	}

	public class getInfo extends TypedInnerClassEndpoint {
		public ComponentInfo f(Component c) {
			return localTwin(c).need(DigitalTwinService.class).info();
		}

		@Override
		public String getDescription() {
			return "get the description for a particular component";
		}
	}


	public class twinComponents extends TypedInnerClassEndpoint {
		public Set<Component> components() {
			Cout.debug("running on " + component + ": " + components);
			return components;
		}

		@Override
		public String getDescription() {
			return "get all known components";
		}
	}


	public class clear extends TypedInnerClassEndpoint {
		public void f() {
			clear();
		}

		@Override
		public String getDescription() {
			return "clear the local view";
		}
	}

	public Collection<Component> lookupByRegexp(String re) {
		return components.stream().filter(e -> e.matches(re)).toList();
	}

	@Override
	public Stream<Info> infos() {
		return components.stream().map(c -> c.need(DigitalTwinService.class)).map(s -> s.info());
	}

	public Component pickRandomPeer() {
		if (components.size() == 0) {
			return null;
		} else {
			var l = new ArrayList<>(components);
			return l.get(ThreadLocalRandom.current().nextInt(l.size()));
		}
	}

	@Override
	public void feedWith(Route route) {
		for (var l : route.entries()) {
			ensureTwinLinkExists(l.link);
		}
	}

	public List<Link> links() {
		return links;
	}

	public Link ensureTwinLinkExists(Link l) {
		localTwin(l.src);
		localTwin(l.dest);

		var twin = links.stream().filter(ll -> ll.equals(l)).findFirst().orElse(null);

		if (twin == null) {
			links.add(twin = l);
		} else {
			twin.latency = l.latency;
			twin.throughput = l.throughput;
			twin.since = l.since;
			twin.date = l.date;
		}

		return twin;
	}

	public void removeOutdated(double now) {
		links.removeIf(n -> n.reliability(now) < minimumAccuracyTolerated);
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
						for (var neighbor : t.outLinks().links()) {
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

	public RRoute findRoute(Component src, Component dest, int maxDistance) {
		return BFS.bfs(src, maxDistance, Integer.MAX_VALUE, c -> false).predecessors.path(src, dest);
	}

	public Routes findDisjointRoutes(Component src, Component dest, int maxDistance, Predicate<Routes> enough) {
		var routes = new Routes();
		routes.disjoint = true;
		var ignore = new HashSet<Component>();
		var bfs = BFS.bfs(src, maxDistance, Integer.MAX_VALUE, c -> false);

		while (true) {
			var r = bfs.predecessors.path(src, dest);

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

	public Component localTwin(Component a) {
		Component twin = components.stream().filter(c -> c.equals(a)).findFirst().orElse(null);

		if (twin == null) {
			components.add(twin = new Component(a.name(), component));
			System.err.println(component + " creates new dt for " + a + " -> " + components);
		}

		var t = twin;
		listeners.forEach(l -> l.newComponent(t));
		return twin;
	}

	public Component lookup(String name) {
		return components.stream().filter(c -> c.matches(name)).findFirst().orElse(null);
	}

	public <S extends Service> S localTwin(Class<S> s, Component child) {
		var twinComponent = localTwin(child);
		var twinService = twinComponent.need(s);

		if (twinService == null) {// the digital twin does have that service, let's add it
			twinService = twinComponent.need(s);
		}

		return twinService;
	}

	public <S extends Service> S localTwin(S s) {
		return localTwin((Class<S>) s.getClass(), s.component);
	}

	public void clear() {
		components.clear();
		links.clear();
	}

	@Override
	public void accept(Info i) {
		if (i instanceof Link) {
			ensureTwinLinkExists((Link) i);
		} else if (i instanceof KNW_NeighborLost) {
			lostLink((KNW_NeighborLost) i);
		} else if (i instanceof ComponentInfo) {
			consider((ComponentInfo) i);
		} else {
			System.err.println("don't know what to do with info of class " + i.getClass().getName());
		}
	}

	private void consider(ComponentInfo d) {
		localTwin(d.component).dt().update(d);
	}

	private void lostLink(KNW_NeighborLost l) {
		localTwin(l.from).outLinks().remove(l.lostNeighbor);
	}

	public Collection<? extends Component> components() {
		return components;
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
}
