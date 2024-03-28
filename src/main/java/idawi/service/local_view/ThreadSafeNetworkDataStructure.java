package idawi.service.local_view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import idawi.Component;
import idawi.transport.Link;
import idawi.transport.TransportService;
import toools.SizeOf;
import toools.Stop;
import toools.collections.Collections;
import toools.io.Cout;

public class ThreadSafeNetworkDataStructure implements Serializable, SizeOf {

	private final List<Component> components = new ArrayList<>();
	private Collection<Link> links = new ArrayList<>();
	public final List<NetworkTopologyListener> listeners = new ArrayList<>();

	public synchronized void clear() {
		components.clear();
		links.clear();
	}

	public void add(Component c) {
		components.add(c);
	}

	@Override
	public synchronized long sizeOf() {
		return SizeOf.sizeOf(components) + SizeOf.sizeOf(links);
	}

	public synchronized Link findLink(TransportService src, TransportService dest, boolean autoCreate,
			Supplier<Link> linkFactory) {
		var existing = search(src, dest);

		if (existing == null && autoCreate) {
			findComponent(c -> c.equals(src.component), true, () -> src.component);
			findComponent(c -> c.equals(dest.component), true, () -> dest.component);
			var l = linkFactory == null ? new Link(src, dest) : linkFactory.get();
			links.add(l);

			for (var li : listeners) {
				li.newLink(l);
			}

			return l;
		} else {
			return existing;
		}
	}

	public synchronized int nbLinks() {
		return links.size();
	}

	public synchronized Component pickRandomComponent(Random r) {
		if (components.size() == 0) {
			return null;
		} else {
			return components.get(r.nextInt(components.size()));
		}
	}

	public synchronized Link pickRandomLink(Random prng) {
		return Collections.pickRandomObject(links, prng);
	}

	public synchronized Collection<Link> pickNRandomLinks(int n, Random r) {
		if (links.size() <= n) {
			return new HashSet<>(links);
		} else {
			return Collections.pickRandomSubset(links, n, false, r);
		}
	}

	public synchronized Link forEachLink(Function<Link, Stop> stop) {
		for (var l : links) {
			if (stop.apply(l) == Stop.yes) {
				return l;
			}
		}

		return null;
	}

	public synchronized <A> List<A> forEachLink_map(Function<Link, A> f) {
		return links.stream().map(l -> f.apply(l)).toList();
	}

	public synchronized Component forEachComponent(Function<Component, Stop> stop) {
		for (var c : components) {
			if (stop.apply(c) == Stop.yes) {
				return c;
			}
		}

		return null;
	}

	public synchronized <A> List<A> forEachComponent_map(Function<Component, A> f) {
		return components.stream().map(l -> f.apply(l)).toList();
	}

	public Link search(Link link) {
		return findLink(l -> l.equals(link));
	}

	public Link search(TransportService src, TransportService dest) {
		return findLink(l -> l.src.equals(src) && l.dest.equals(dest));
	}

	public Link findLink(Predicate<Link> p) {
		return forEachLink(l -> Stop.stopIf(p.test(l)));
	}

	public synchronized Component findComponent(Predicate<Component> p, boolean autoCreate,
			Supplier<Component> componentFactory) {
		var c = forEachComponent(a -> Stop.stopIf(p.test(a)));

		if (c == null && autoCreate) {
			if (componentFactory != null) {
				c = componentFactory.get();
			}

			components.add(c);

			for (var l : listeners)
				l.newComponent(c);
		}

		return c;
	}

}
