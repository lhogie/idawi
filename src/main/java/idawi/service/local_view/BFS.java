package idawi.service.local_view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import idawi.Component;
import idawi.transport.Link;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

public class BFS {
	public static class BFSResult {
		public Object2LongMap<Component> distances = new Object2LongOpenHashMap<>();
		public PredecessorTable predecessors;
		public int nbVerticesVisited;
		public List<Link> visitOrder = new ArrayList<>();

		public BFSResult(Component src) {
			this.predecessors = new PredecessorTable(src);
		}

		@Override
		public String toString() {
			return "BFS [distances=" + distances + "]";
		}
	}

	public static class RRoute extends ArrayList<Link> implements Infoable {
		Info info = new Info() {
			@Override
			public void exposeComponent(Predicate<Component> p) {
				for (var c : RRoute.this) {
					if (p.test(c.src.component) || p.test(c.dest.component)) {
						return;
					}
				}
			}

			@Override
			public double reliability(double now) {
				double r = 1;

				for (var i : RRoute.this) {
					r *= i.reliability(now);
				}

				return r;
			}
		};

		public void reverse() {
			Collections.reverse(this);
		}

		@Override
		public Info asInfo() {
			return info;
		}

		public Link last() {
			return get(size() - 1);
		}
	}

	public static class Routes extends Info {
		private ArrayList<RRoute> routes = new ArrayList<RRoute>();
		public boolean disjoint;

		@Override
		public void exposeComponent(Predicate<Component> p) {
			routes.forEach(r -> r.asInfo().exposeComponent(p));
		}

		@Override
		public double reliability(double now) {
			double r = 1;

			for (var i : routes) {
				r *= i.asInfo().reliability(now);
			}

			return r;
		}

		public double successRate(double now) {
			// more routes increase successRate
			return 1 - routes.stream().map(r -> 1 - r.asInfo().reliability(now)).reduce((a, b) -> a * b).get();
		}

		public void add(RRoute newRoute) {
			if (disjoint) {
				for (var newLink : newRoute) {
					for (var alreadyKnownRoute : routes) {
						if (alreadyKnownRoute.contains(newLink)) {
							throw new IllegalArgumentException(
									"link " + newLink + " already used in " + alreadyKnownRoute);
						}
					}
				}
			}

			routes.add(newRoute);
		}

		public RRoute findARouteUsing(Link l) {
			for (var alreadyKnownRoute : routes) {
				if (alreadyKnownRoute.contains(l)) {
					return alreadyKnownRoute;
				}
			}

			return null;
		}
	}

	public static BFSResult bfs(Network links, Component source, long maxDistance, long maxNbVerticesVisited,
			Predicate<Component> ignoreComponent, Predicate<Link> ignoreLink) {
		BFSResult r = new BFSResult(source);
		var q = new ArrayList<Component>();
		q.add(source);
		r.distances.put(source, 0L);

		while (!q.isEmpty()) {
			var c = q.remove(0);
			var d = r.distances.getLong(c);

			if (!ignoreComponent.test(c)) {
				links.findLinks(l -> l.src.component.equals(c)).forEach(l -> {
					if (r.visitOrder != null) {
						r.visitOrder.add(l);
					}

					if (!ignoreLink.test(l) && l.isActive()) {
						var succ = l.dest.component;

						if (!r.distances.containsKey(succ)) {
							r.distances.put(succ, d + 1);
							r.predecessors.put(succ, l);
							q.add(succ);
						}
					} else {
					}
				});
			}
		}

		return r;
	}
}
