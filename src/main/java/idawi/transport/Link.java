package idawi.transport;

import java.util.function.Predicate;

import idawi.Component;
import idawi.RuntimeEngine;
import idawi.service.local_view.Info;

public class Link extends Info {
	// the network interface on the neighbor side
	public TransportService src, dest;
	public final Activity activity = new Activity();

	public double latency = 0.001;
	public int throughput;

	public Link() {
		activity.add(new TimeFrame(RuntimeEngine.now()));
	}

	@Override
	public long sizeOf() {
		return super.sizeOf() + 32 + activity.sizeOf();
	}

	public boolean isActive() {
		return activity.available();
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

	public void dispose() {
		src.dispose(this);
		dest.dispose(this);
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public boolean matches(TransportService from, TransportService to) {
		return (from == null || from.equals(this.src)) && (dest == null || to.equals(this.dest));
	}

	public boolean matches(Component from, Component to) {
		return (from == null || from.equals(this.src.component)) && (dest == null || to.equals(this.dest.component));
	}

}