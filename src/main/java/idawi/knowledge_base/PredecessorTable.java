package idawi.knowledge_base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PredecessorTable extends HashMap<ComponentRef, ComponentRef> {
	public List<ComponentRef> path(ComponentRef source, ComponentRef dest) {
		var r = new ArrayList<ComponentRef>();
		r.add(dest);

		while (true) {
			var p = get(r.get(r.size() - 1));

			if (p == null) {
				return null;
			} else if (p.equals(source)) {
				Collections.reverse(r);
				return r;
			}

			r.add(p);
		}
	}

	public Set<ComponentRef> successors(ComponentRef source, Set<ComponentRef> dest) {
		var r = new HashSet<ComponentRef>();

		for (var d : dest) {
			var p = path(source, d);

			if (p != null) {
				r.add(p.get(0));
			}
		}

		return r;
	}

	public int distance(ComponentRef from, ComponentRef to) {
		return path(from, to).size();
	}

}