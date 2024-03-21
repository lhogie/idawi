package idawi.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import idawi.Component;
import idawi.service.LocationService;
import idawi.service.local_view.Network;
import idawi.transport.delauney2.Delauney2;
import idawi.transport.delauney2.Point;
import toools.collection.Pair;
import toools.function.TriConsumer;

public class Topologies {

	public static void delauneyTriangulation(List<Component> components,
			BiFunction<Component, Component, Class<? extends TransportService>> f, Collection<Component> inform) {
		Delauney2 delaunay = new Delauney2();

		class P extends Point {
			Component c;

			P(Component c) {
				var l = c.getLocation();
				this.x = l.x;
				this.y = l.y;
				this.c = c;
			}
		}

		components.forEach(c -> delaunay.insertPoint(new P(c)));

		for (var edge : delaunay.computeEdges()) {
			var a = (Component) ((P) edge[0]).c;
			var b = (Component) ((P) edge[1]).c;
			consider(a, b, f.apply(a, b), inform);
			consider(b, a, f.apply(b, a), inform);
		}
	}

	public static void connect2Nclosests(List<Component> components, int n,
			BiFunction<Component, Component, Class<? extends TransportService>> f, Collection<Component> inform) {

		for (var c : components) {
			for (var b : sortByDistanceTo(components, c).subList(1, 1 + n)) {
				consider(c, b, f.apply(c, b), inform);
			}
		}
	}

	public static class DistancedPair extends Pair<Component> {
		double distance;

		DistancedPair(Component a, Component b) {
			super(a, b, false);
			this.distance = distance(a, b);
		}
	}

	public static List<DistancedPair> pairsSortedByDistance(Collection<Component> components) {
		var pairs = new ArrayList<DistancedPair>();
		var componentList = new ArrayList<>(components);
		int len = componentList.size();

		for (int i = 0; i < len; ++i) {
			var a = componentList.get(i);

			for (int j = i + 1; j < len; ++j) {
				var b = componentList.get(j);
				pairs.add(new DistancedPair(a, b));
			}
		}

		Collections.sort(pairs, (p1, p2) -> Double.compare(p1.distance, p2.distance));
		return pairs;
	}

	public static List<Component> sortByDistanceTo(List<Component> l, Component from) {
		var r = new ArrayList<>(l);
		Collections.sort(r, (a, b) -> Double.compare(distance(from, a), distance(from, b)));
		return r;
	}

	public static List<Component> sortByDistanceTo2(Component from, List<Component> l) {
		class E {
			Component c;
			double d;
		}

		var r = new ArrayList<E>();

		for (var a : l) {
			E e = new E();
			e.c = a;
			e.d = distance(from, a);
		}

		Collections.sort(r, (a, b) -> Double.compare(a.d, b.d));
		return r.stream().map(e -> e.c).toList();
	}

	private static void consider(Component a, Component b, Class<? extends TransportService> t,
			Collection<Component> inform) {
		if (t != null) {
			Network.markLinkActive(a, b, t, false, inform);
		}
	}

	public static void chain(List<Component> components,
			BiFunction<Component, Component, Class<? extends TransportService>> f, Collection<Component> inform) {
		int n = components.size();

		for (int i = 1; i < n; ++i) {
			var a = components.get(i - 1);
			var b = components.get(i);
			consider(a, b, f.apply(a, b), inform);
			consider(b, a, f.apply(a, b), inform);
		}
	}


	public static void dchain(List<Component> components,
			BiFunction<Component, Component, Class<? extends TransportService>> f, Collection<Component> inform) {
		int n = components.size();

		for (int i = 1; i < n; ++i) {
			var a = components.get(i - 1);
			var b = components.get(i);
			consider(a, b, f.apply(a, b), inform);
		}
	}

	public static void planar(List<Component> components, Random r,
			BiFunction<Component, Component, Class<? extends TransportService>> f, Collection<Component> inform) {
		{ // start with 1 link
			var a = components.get(0);
			var b = components.get(1);
			consider(a, b, f.apply(a, b), inform);
		}

		for (int i = 2; i < components.size(); ++i) {
			var newComponent = components.get(i);
			var a = components.get(r.nextInt(i));
			var b = a.localView().g.findALink(l -> l.src.component.equals(a)).dest.component;
			consider(a, newComponent, f.apply(a, newComponent), inform);
			consider(newComponent, b, f.apply(newComponent, b), inform);
		}
	}

	public static void chains(Collection<Component> components, int n,
			BiFunction<Component, Component, Class<? extends TransportService>> f, Consumer<List<Component>> alter,
			Collection<Component> inform) {

		var l = new ArrayList<>(components);

		for (int i = 0; i < n; ++i) {
			alter.accept(l);
			chain(l, f, inform);
		}
	}

	public static void chains(Collection<Component> components, int n, Random r,
			BiFunction<Component, Component, Class<? extends TransportService>> f, Collection<Component> inform) {
		chains(components, n, f, l -> Collections.shuffle(l, r), inform);
	}

	public static void gnm(List<Component> components, int nbEdges, Random r,
			BiFunction<Component, Component, Class<? extends TransportService>> f) {

		while (nbEdges > 0) {
			final var a = toools.collections.Collections.pickRandomObject(components, r);
			components.get(ThreadLocalRandom.current().nextInt(components.size()));
			var b = a;

			while (b == a || a.localView().g.findALinkConnecting(a, b) != null) {
				b = toools.collections.Collections.pickRandomObject(components, r);
			}

			consider(a, b, f.apply(a, b), components);
		}
	}

	public static double gnp(Collection<Component> components, double p, Random r,
			BiFunction<Component, Component, Class<? extends TransportService>> f, boolean bidi, double timeIncrement,
			Collection<Component> inform) {
		double time = 0;

		for (var a : components) {
			for (var b : components) {
				if (a != b) {
					var transport = f.apply(a, b);

					if (transport != null && r.nextDouble() < p && !a.service(transport, true).activeOutLinks().stream()
							.anyMatch(l -> l.dest.component.equals(b))) {
						Network.markLinkActive(a, b, transport, false, inform);
					}
				}
			}
		}

		return time;
	}

	public static class TreeO {
		public Class<? extends TransportService> leaf2tree, tree2leaf;
	}

	public static void tree(List<Component> components, TriConsumer<Component, Component, TreeO> tc,
			Collection<Component> inform, Random prng) {
		final int n = components.size();
		final var out = new TreeO();

		for (int i = 1; i < n; ++i) {
			var leaf = components.get(i);
			var parent = components.get(prng.nextInt(i));
			tc.accept(parent, leaf, out);
			consider(leaf, parent, out.leaf2tree, inform);
			consider(parent, leaf, out.tree2leaf, inform);
		}
	}

	public static void clique(Collection<Component> components,
			BiFunction<Component, Component, Class<? extends TransportService>> f, Collection<Component> inform) {
		for (var a : components) {
			for (var b : components) {
				if (a != b) {
					consider(a, b, f.apply(a, b), inform);
				}
			}
		}
	}

	public static void wirelessMesh(List<Component> components,
			BiFunction<Component, Component, Class<? extends WirelessTransport>> tt, Collection<Component> inform) {
		// remove invalid links
		for (var c : components) {
			c.localView().g
					.findLinks(l -> l.src instanceof WirelessTransport
							&& distance(l.src.component, l.dest.component) > ((WirelessTransport) l.src).emissionRange)
					.forEach(l -> c.localView().g.deactivateLink(l));
		}

		// create new links
		for (var a : components) {
			for (var b : components) {
				if (a != b) {
					final var d = distance(a, b);
					var transportClass = tt.apply(a, b);
					var ta = a.service(transportClass, true);

					if (d < ta.emissionRange) {
						var tb = b.service(transportClass, true);
						Network.markLinkActive(ta, tb, false, inform);
					}
				}
			}
		}
	}

	public static double distance(Component a, Component b) {
		var aLoc = a.service(LocationService.class, true).location;
		var bLoc = b.service(LocationService.class, true).location;
		return aLoc.distanceFrom(bLoc);
	}

}
