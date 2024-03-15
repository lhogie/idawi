package idawi.routing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Stream;

import idawi.Component;
import idawi.transport.Link;
import idawi.transport.TransportService;
import toools.SizeOf;

public class Route extends ArrayList<Entry> implements SizeOf {

	private static final long serialVersionUID = 1L;

	public boolean add(Entry newEntry) {
		if (!isEmpty() && !last().link.dest.component.equals(newEntry.link.src.component))
			throw new IllegalArgumentException("you try to add an entry that not chain");

		return super.add(newEntry);
	}

	public static class ComponentSequence extends ArrayList<Component> {
		public ComponentSequence distinct() {
			return stream().distinct().collect(collector());
		}

		public static Collector<Component, ComponentSequence, ComponentSequence> collector() {
			return Collector.of(ComponentSequence::new, (c, t) -> c.add(t), (left, right) -> {
				left.addAll(right);
				return left;
			});
		}
	}

	public ComponentSequence components() {
		var components = new ComponentSequence();
		forEach(e -> components.add(e.link.src.component));
		components.add(last().link.dest.component);
		return components;
	}

	public Stream<TransportService> recipients() {
		return stream().map(e -> e.link.dest);
	}

	public Set<Class<? extends TransportService>> transportClasses() {
		var r = new HashSet<Class<? extends TransportService>>();
		forEach(e -> {
			r.add(e.link.src.getClass());
			r.add(e.link.dest.getClass());
		});
		return r;
	}

	public Entry getr(int rewind) {
		if (isEmpty())
			throw new IllegalStateException("route is empty");

		return get(size() - 1 - rewind);
	}

	public Entry last() {
		return getr(0);
	}

	public List<TransportService> transports() {
		var r = new ArrayList<TransportService>();

		for (var e : this) {
			r.add(e.link.src);
			r.add(e.link.dest);
		}

		return r;
	}

	public Component source() {
		return get(0).link.src.component;
	}

	public int len() {
		return size();
	}

	public double duration() {
		return last().receptionDate - get(0).emissionDate;
	}

	public boolean isEmpty() {
		return len() == 0;
	}

	public Entry first() {
		return get(0);
	}

	public void add(Link l, RoutingService p) {
		add(new Entry(l, p.getClass()));
	}

	public long howManyTimesFound(Component component) {
		return components().stream().filter(c -> c.equals(component)).count();
	}

	@Override
	public long sizeOf() {
		return SizeOf.sizeOf(this);
	}
}
