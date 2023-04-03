package idawi.knowledge_base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import idawi.knowledge_base.BFS.RRoute;
import idawi.transport.OutNeighbor;

public class PredecessorTable extends HashMap<OutNeighbor, OutNeighbor> {
	public RRoute path(Component source, Component dest) {
		var r = new RRoute();
		r.add(find(dest));

		while (true) {
			var p = get(r.get(r.size() - 1));

			if (p == null) {
				return null;
			} else if (p.transport.component.equals(source)) {
				r.reverse();
				return r;
			}

			r.add(p);
		}
	}

	private OutNeighbor find(Component c) {
		for (var on : keySet()) {
			if (on.transport.component.equals(c)) {
				return on;
			}
		}

		return null;
	}

	public Set<OutNeighbor> successors(Component source, Set<Component> dest) {
		var r = new HashSet<OutNeighbor>();

		for (var d : dest) {
			var p = path(source, d);

			if (p != null) {
				r.add(p.get(0));
			}
		}

		return r;
	}

	public int distance(Component from, Component to) {
		return path(from, to).size();
	}

}