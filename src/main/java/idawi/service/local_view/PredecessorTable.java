package idawi.service.local_view;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import idawi.Component;
import idawi.service.local_view.BFS.RRoute;
import idawi.transport.Link;

public class PredecessorTable extends HashMap<Link, Link> {
	public RRoute path(Component source, Component dest) {
		var r = new RRoute();
		r.add(find(dest));

		while (true) {
			var p = get(r.get(r.size() - 1));

			if (p == null) {
				return null;
			} else if (p.dest.component.equals(source)) {
				r.reverse();
				return r;
			}

			r.add(p);
		}
	}

	private Link find(Component c) {
		for (var on : keySet()) {
			if (on.dest.component.equals(c)) {
				return on;
			}
		}

		return null;
	}

	public Set<Link> successors(Component source, Set<Component> dest) {
		var r = new HashSet<Link>();

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