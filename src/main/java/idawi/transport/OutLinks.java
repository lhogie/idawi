package idawi.transport;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import idawi.Component;
import idawi.service.local_view.Info;

public class OutLinks extends Info implements Serializable, Iterable<Link> {

	Collection<Link> links = new HashSet<>();

	public OutLinks(Collection<Link> s) {
		this.links =s;
	}
	
	
	@Override
	public void exposeComponent(Predicate<Component> c) {
		for (var i : links) {
			if (c.test(i.dest.component)) {
				return;
			}
		}
	}

	public Collection<Link> links() {
		return links;
	}

	public List<Component> destinations() {
		return links.stream().map(i -> i.dest.component).toList();
	}

	public int size() {
		return links.size();
	}

	public boolean contains(Component c) {
		for (var a : destinations()) {
			if (a.equals(c)) {
				return true;
			}
		}

		return false;
	}

	public Stream<Link> stream() {
		return links.stream();
	}

	public Link search(Component to) {
		for (var t : links) {
			if (t.dest.component.equals(to)) {
				return t;
			}
		}

		return null;
	}

	public void add(Link newI) {
		links.add(newI);
	}

	public Link search(TransportService to) {
		for (var t : links) {
			if (t.dest.equals(to)) {
				return t;
			}
		}

		return null;
	}

	public void remove(TransportService t) {
		links.removeIf(n -> n.dest.equals(t));
	}

	public void remove(Component c) {
		links.removeIf(n -> n.dest.component.equals(c));
	}

	public void remove(Component lostNeighbor, Class<? extends TransportService> lostNeighborT) {
		links.removeIf(n -> n.dest.component.equals(lostNeighbor) && n.dest.getClass() == lostNeighborT);
	}

	@Override
	public Iterator<Link> iterator() {
		return links.iterator();
	}

	public static OutLinks merge(OutLinks... neighborhoods) {
		var r = new OutLinks(new HashSet<>());

		for (var neighborhood : neighborhoods) {
			for (var neighbor : neighborhood) {
				r.add(neighbor);
			}
		}

		return r;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += ", out neighbors: " + this.links;
		return s;
	}

}
