package idawi.knowledge_base;

import java.util.ArrayList;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import toools.io.Cout;

public class BFS {
	public static class BFSResult {
		public Object2LongMap<ComponentRef> distances = new Object2LongOpenHashMap<>();
		public PredecessorTable predecessors = new PredecessorTable();
		public int nbVerticesVisited;

		@Override
		public String toString() {
			return "BFS [distances=" + distances + "]";
		}
	}

	public static BFSResult bfs(NetworkMap g, ComponentRef source, long maxDistance, long maxNbVerticesVisited) {
		Cout.debugSuperVisible("bfs");
		BFSResult r = new BFSResult();

//		AtomicBoolean completed = new AtomicBoolean(false);

		// Threads.newThread_loop_periodic(1000, () -> completed.get(), () -> {
		// reply(triggerMsg, new ProgressRatio(nbVertices, r.nbVerticesVisited));
		// });

//		var lp = new LongProcess("BFS (classic)", " vertex", nbVertices);
		var q = new ArrayList<ComponentRef>();
		q.add(source);
		r.distances.put(source, 0L);
		long nbVerticesVisited = 1;

		while (!q.isEmpty()) {
			Cout.debug(q.size() + " elements in queue");
//			++lp.sensor.progressStatus;

			var v = q.remove(0);
			var d = r.distances.getLong(v);

			if (d <= maxDistance) {
				for (var e : g.outs(v)) {
					if (!r.distances.containsKey(e.dest)) {
						r.distances.put(e.dest, d + 1);
						r.predecessors.put(v, e.dest);

						if (nbVerticesVisited++ >= maxNbVerticesVisited)
							break;

						q.add(e.dest);
					}
				}
			}
		}

		Cout.debugSuperVisible("bfs completed");

		return r;
	}
}
