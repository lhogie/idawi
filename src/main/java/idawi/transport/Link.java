package idawi.transport;

import java.util.function.Predicate;

import idawi.Component;
import idawi.service.local_view.Info;

public class Link extends Info {
	// the network interface on the neighbor side
	public TransportService src, dest;
	public double since;
	public double latency;
	public int throughput;

	public Link() {
	}

	public Link(TransportService from, TransportService to) {
		this.src = from;
		this.dest = to;
	}

	@Override
	public void exposeComponent(Predicate<Component> p) {
		p.test(dest.component);
	}

	@Override
	public String toString() {
		if (src.getClass() == dest.getClass()) {
			return src.component + " ==" + src.getName() + "==> " + dest.component;
		} else {
			return src + " => " + dest;
		}
	}

	@Override
	public boolean equals(Object o) {
		var l = (Link) o;
		return src.equals(l.src) && dest.equals(l.dest);
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public boolean matches(TransportService from, Class<? extends TransportService> protocol, Component to) {
		return (from == null || from.equals(src)) && (dest == null || dest.outLinks().contains(to));
	}

	public boolean matches(Component from, Class<? extends TransportService> protocol, Component to) {
		return (from == null || from.equals(src.component)) && (dest == null || dest.outLinks().contains(to));
	}

}