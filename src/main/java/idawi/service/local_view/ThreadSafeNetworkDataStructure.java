package idawi.service.local_view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

import idawi.Component;
import idawi.transport.Link;
import toools.SizeOf;
import toools.Stop;
import toools.collections.Collections;

public class ThreadSafeNetworkDataStructure implements Serializable, SizeOf {

	private final List<Component> components = new ArrayList<>();
	private Collection<Link> links = new ArrayList<>();
	public final List<NetworkTopologyListener> listeners = new ArrayList<>();

	public synchronized void clear() {
		components.clear();
		links.clear();
	}

	@Override
	public synchronized long sizeOf() {
		return SizeOf.sizeOf(components) + SizeOf.sizeOf(links);
	}

	public synchronized Link ensureExists(final Link l) {
		var existing = search(l);

		if (existing == null) {
			l.src.component = ensureExists(l.src.component);
			l.dest.component = ensureExists(l.dest.component);
			links.add(existing = l);
			return l;
		} else {
			return existing;
		}
	}

	public synchronized Component ensureExists(Component c) {
		var existing = find(c);

		if (existing == null) {
			components.add(c);
			listeners.forEach(l -> l.newComponent(c));
			return c;
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
		return findALink(l -> l.equals(link));
	}

	public Link findALink(Predicate<Link> p) {
		return forEachLink(l -> Stop.stopIf(p.test(l)));
	}

	public Component findAComponent(Predicate<Component> p) {
		return forEachComponent(l -> Stop.stopIf(p.test(l)));
	}

	public Component find(Component a) {
		return findAComponent(b -> b.publicKey().equals(a.publicKey()));
	}

}
