package idawi.transport;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import idawi.Component;
import idawi.service.local_view.Info;

public class OutLinks extends Info implements Serializable, Iterable<Link> {

	public Collection<Link> set;

	public OutLinks() {
		this(new HashSet<>());
	}

	public OutLinks(Collection<Link> s) {
		this.set = s;
	}

	public OutLinks(Stream<Link> s) {
		this();
		s.forEach(l -> set.add(l));
	}

	@Override
	public void exposeComponent(Predicate<Component> c) {
		for (var i : set) {
			if (c.test(i.dest.component)) {
				return;
			}
		}
	}

	public Collection<Link> links() {
		return set;
	}

	public List<Component> destinations() {
		return set.stream().map(i -> i.dest.component).toList();
	}

	public int size() {
		return set.size();
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
		return set.stream();
	}

	public Link search(Component to) {
		for (var t : set) {
			if (t.dest.component.equals(to)) {
				return t;
			}
		}

		return null;
	}

	public void add(Link newI) {
		set.add(newI);
	}

	public Link search(TransportService to) {
		for (var t : set) {
			if (t.dest.equals(to)) {
				return t;
			}
		}

		return null;
	}

	public void removeLinksHeadingTo(TransportService t) {
		set.removeIf(n -> n.dest.equals(t));
	}

	public void remove(Component c) {
		set.removeIf(n -> n.dest.component.equals(c));
	}

	public void remove(Link l) {
		l.dispose();
		set.remove(l);
	}

	public void remove(Component lostNeighbor, Class<? extends TransportService> lostNeighborT) {
		set.removeIf(n -> n.dest.component.equals(lostNeighbor) && n.dest.getClass() == lostNeighborT);
	}

	@Override
	public Iterator<Link> iterator() {
		return set.iterator();
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
		s += ", out neighbors: " + this.set;
		return s;
	}

	public void clear() {
		set.clear();

	}

	public Collection<Link> outLinks(Component c) {
		return stream().filter(l -> l.src.component.equals(c)).toList();
	}

}
