package idawi.service.local_view;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import idawi.Component;
import idawi.service.local_view.BFS.RRoute;
import idawi.transport.Link;

public class PredecessorTable extends LinkedHashMap<Component, Link> {
	final Component src;

	public PredecessorTable(Component src) {
		this.src = src;
	}

	public RRoute pathTo(Component dest) {
		var r = new RRoute();

		while (!dest.equals(src)) {
			var l = get(dest);

			if (l == null) {
				return null;
			} else {
				r.add(l);
				dest = l.src.component;
			}
		}

		r.reverse();
		return r;
	}

	public List<RRoute> allPaths() {
		return keySet().stream().map(c -> pathTo(c)).toList();
	}

	public Set<Link> relaysTo(Set<Component> dest) {
		var r = new HashSet<Link>();

		for (var d : dest) {
			var p = pathTo(d);

			if (p != null) {
				r.add(p.get(0));
			}
		}

		return r;
	}

	public int distanceTo(Component c) {
		return pathTo(c).size();
	}

}