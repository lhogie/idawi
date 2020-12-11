package idawi;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph<V> implements Serializable {
	private final Map<V, Set<V>> m = new HashMap<>();

	public void add(V u, V v) {
		Set<V> s = m.get(u);

		if (s == null) {
			m.put(u, s = new HashSet<>());
		}

		s.add(v);
	}

	public Set<V> get(V u) {
		return m.containsKey(u) ? m.get(u) : Collections.EMPTY_SET;
	}

	public void remove(V u, V v) {
		Set<V> s = m.get(u);

		if (s == null) {
			throw new IllegalStateException("no such vertex: " + u);
		}

		if (!s.remove(v)) {
			throw new IllegalStateException("no such edge");
		}

		if (s.isEmpty()) {
			m.remove(u);
		}
	}

	public List<V> bfs(V u) {
		List<V> visit = new LinkedList<>();
		List<V> q = new LinkedList<>();
		q.add(u);

		while (!q.isEmpty()) {
			u = q.remove(0);
			visit.add(u);

			for (V v : get(u)) {
				if (!visit.contains(v)) {
					q.add(v);
				}
			}
		}

		return visit;
	}
}
