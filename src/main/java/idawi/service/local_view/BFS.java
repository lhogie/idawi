package idawi.service.local_view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;

import idawi.Component;
import idawi.transport.Link;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import toools.io.Cout;

public class BFS {
	public static class BFSResult {
		public Object2LongMap<Component> distances = new Object2LongOpenHashMap<>();
		public PredecessorTable predecessors = new PredecessorTable();
		public int nbVerticesVisited;

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
					if (p.test(c.dest.component)) {
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

		public RRoute findRouteUsing(Link l) {
			for (var alreadyKnownRoute : routes) {
				if (alreadyKnownRoute.contains(l)) {
					return alreadyKnownRoute;
				}
			}

			return null;
		}

	}

	public static BFSResult bfs(Component source, long maxDistance, long maxNbVerticesVisited,
			Predicate<Link> ignore) {
		Cout.debugSuperVisible("bfs");
		BFSResult r = new BFSResult();

//		AtomicBoolean completed = new AtomicBoolean(false);

		// Threads.newThread_loop_periodic(1000, () -> completed.get(), () -> {
		// reply(triggerMsg, new ProgressRatio(nbVertices, r.nbVerticesVisited));
		// });

//		var lp = new LongProcess("BFS (classic)", " vertex", nbVertices);
		var q = new ArrayList<Link>();

		for (var n : source.outLinks()) {
			q.add(n);
			r.distances.put(n.dest.component, 1L);
		}

		long nbVerticesVisited = 1;

		while (!q.isEmpty()) {
			Cout.debug(q.size() + " elements in queue");
//			++lp.sensor.progressStatus;

			var n = q.remove(0);

			if (!ignore.test(n)) {
				for (var transport : n.dest.component.services(TransportService.class)) {
					for (var outNeighbor : transport.outLinks()) {
						var d = r.distances.getLong(n);

						if (!r.distances.containsKey(outNeighbor)) {
							r.distances.put(outNeighbor.dest.component, d + 1);
							r.predecessors.put(n, outNeighbor);

							if (nbVerticesVisited++ >= maxNbVerticesVisited)
								break;

							q.add(outNeighbor);
						}
					}
				}
			}
		}

		Cout.debugSuperVisible("bfs completed");

		return r;
	}
}
