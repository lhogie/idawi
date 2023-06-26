package idawi.routing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Stream;

import idawi.Component;
import idawi.transport.Link;
import idawi.transport.TransportService;

public class Route implements Serializable {

	public static class Entry implements Serializable {
		public Link link;
		public double emissionDate, receptionDate;
		public Class<? extends RoutingService> routing;
		public RoutingData routingParms;

		public Entry(Link l, Class<? extends RoutingService> routing) {
			this.link = l;
			this.routing = routing;
		}

		public double duration() {
			return receptionDate - emissionDate;
		}

		public RoutingData routingParms() {
			return routingParms;
		}

		@Override
		public String toString() {
			return link.toString();
		}

		public Class<? extends RoutingService> routingProtocol() {
			return routing;
		}
	}

	private static final long serialVersionUID = 1L;

	private final ArrayList<Entry> entries = new ArrayList<>();

	public void add(Entry r) {
		if (!isEmpty() && !entries.get(0).link.dest.component.equals(r.link.src.component))
			throw new IllegalArgumentException("invalid route");

		entries.add(r);
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
		entries.forEach(e -> components.add(e.link.src.component));
		components.add(last().link.dest.component);
		return components;
	}
	
	public Stream<TransportService> recipients() {
		return entries.stream().map(e -> e.link.dest);
	}

	public Set<Class<? extends TransportService>> transportClasses() {
		var r = new HashSet<Class<? extends TransportService>>();
		entries.forEach(e -> {
			r.add(e.link.src.getClass());
			r.add(e.link.dest.getClass());
		});
		return r;
	}

	public Entry last() {
		if (isEmpty())
			throw new IllegalStateException("route is empty");

		return entries.get(entries.size() - 1);
	}

	public List<TransportService> transports() {
		var r = new ArrayList<TransportService>();

		for (var e : entries) {
			r.add(e.link.src);
			r.add(e.link.dest);
		}

		return r;
	}

	public Component source() {
		return entries.get(0).link.src.component;
	}

	@Override
	public String toString() {
		return entries.toString();
	}

	public int len() {
		return entries.size();
	}

	public double duration() {
		return last().receptionDate - entries.get(0).emissionDate;
	}

	public boolean isEmpty() {
		return len() == 0;
	}

	public void removeLast() {
		entries.remove(len() - 1);
	}

	public Entry first() {
		return entries.get(0);
	}

	public void add(Link l, RoutingService p) {
		entries.add(new Entry(l, p.getClass()));
	}

	public List<Entry> entries() {
		return entries;
	}

	public long howManyTimesFound(Component component) {
		return components().stream().filter(c -> c.equals(component)).count();
	}
}
