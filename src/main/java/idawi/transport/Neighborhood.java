package idawi.transport;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import idawi.Component;
import idawi.knowledge_base.Info;

public class Neighborhood extends Info implements Serializable, Iterable<OutNeighbor> {

	Set<OutNeighbor> s = new HashSet<>();

	@Override
	public void exposeComponent(Predicate<Component> c) {
		for (var i : s) {
			if (c.test(i.transport.component)) {
				return;
			}
		}
	}

	public Set<OutNeighbor> infos() {
		return s;
	}

	public List<Component> components() {
		return s.stream().map(i -> i.transport.component).toList();
	}

	public int size() {
		return s.size();
	}

	public boolean contains(Component c) {
		for (var a : components()) {
			if (a.equals(c)) {
				return true;
			}
		}

		return false;
	}

	public Stream<OutNeighbor> stream() {
		return s.stream();
	}

	public OutNeighbor search(Component to) {
		for (var t : s) {
			if (t.transport.component.equals(to)) {
				return t;
			}
		}

		return null;
	}

	public void add(OutNeighbor newI) {
		s.add(newI);
	}

	public OutNeighbor search(TransportService to) {
		for (var t : s) {
			if (t.transport.equals(to)) {
				return t;
			}
		}

		return null;
	}

	public void remove(TransportService lostNeighbor) {
		s.removeIf(n -> n.transport.equals(lostNeighbor));
	}

	@Override
	public Iterator<OutNeighbor> iterator() {
		return s.iterator();
	}

	
	public static Neighborhood merge(Neighborhood... neighborhoods) {
		var r = new Neighborhood();
		
		for (var neighborhood : neighborhoods) {
			for (var neighbor : neighborhood) {
				r.add(neighbor);
			}
		}
		
		return r;
	}
}
