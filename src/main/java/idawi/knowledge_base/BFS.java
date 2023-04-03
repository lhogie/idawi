package idawi.knowledge_base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;

import idawi.Component;
import idawi.transport.OutNeighbor;
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

	public static class RRoute extends Info {
		ArrayList<OutNeighbor> l = new ArrayList<OutNeighbor>();

		@Override
		public void exposeComponent(Predicate<Component> p) {
			for (var c : l) {
				if (p.test(c.dest.component)) {
					return;
				}
			}
		}

		@Override
		public double reliability(double now) {
			double r = 1;

			for (var i : l) {
				r *= i.reliability(now);
			}

			return r;
		}

		public int size() {
			return l.size();
		}

		public OutNeighbor get(int i) {
			return l.get(i);
		}

		public void reverse() {
			Collections.reverse(l);
		}

		public void add(OutNeighbor dest) {
			l.add(dest);

		}
	}

	public static class Routes extends Info {
		ArrayList<RRoute> routes = new ArrayList<RRoute>();

		@Override
		public void exposeComponent(Predicate<Component> p) {
			routes.forEach(r -> r.exposeComponent(p));
		}

		@Override
		public double reliability(double now) {
			double r = 1;

			for (var i : routes) {
				r *= i.reliability(now);
			}

			return r;
		}

		public double successRate(double now) {
			// more routes increase successRate
			return 1 - routes.stream().map(r -> 1 - r.reliability(now)).reduce((a, b) -> a * b).get();
		}

		public void add(RRoute r) {
			routes.add(r);
		}
	}

	public static BFSResult bfs(Component source, long maxDistance, long maxNbVerticesVisited,
			Predicate<OutNeighbor> ignore) {
		Cout.debugSuperVisible("bfs");
		BFSResult r = new BFSResult();

//		AtomicBoolean completed = new AtomicBoolean(false);

		// Threads.newThread_loop_periodic(1000, () -> completed.get(), () -> {
		// reply(triggerMsg, new ProgressRatio(nbVertices, r.nbVerticesVisited));
		// });

//		var lp = new LongProcess("BFS (classic)", " vertex", nbVertices);
		var q = new ArrayList<OutNeighbor>();

		for (var n : source.neighbors()) {
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
					for (var outNeighbor : transport.neighborhood()) {
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
