package idawi.transport;

import java.util.Objects;
import java.util.function.Predicate;

import idawi.Component;
import idawi.Agenda;
import idawi.service.local_view.Info;

public class Link extends Info {
	// the network interface on the neighbor side
	public final TransportService src, dest;
	private boolean active = false;
	public double latency;
	public int throughput;
	public final Activity activity = new Activity();

	public Link(TransportService from, TransportService to) {
		Objects.requireNonNull(this.src = from);
		Objects.requireNonNull(this.dest = to);
	}

	public boolean headsTo(Component c) {
		return c.equals(dest.component);
	}

	@Override
	public final boolean involves(Component d) {
		return src.component.equals(d) || dest.component.equals(d);
	}

	@Override
	public long sizeOf() {
		return super.sizeOf() + 32 + activity.sizeOf();
	}

	public boolean isActive() {
		return active;// activity.available();
	}

	@Override
	public void exposeComponent(Predicate<Component> p) {
		p.test(dest.component);
	}

	@Override
	public String toString() {
		var t = getCommonTransport();

		if (t != null) {
			return src.component + " ==" + src.getName() + "==> " + dest.component;
		} else {
			return src + " => " + dest;
		}
	}

	public Class<? extends TransportService> getCommonTransport() {
		if (src.getClass() == dest.getClass()) {
			return src.getClass();
		} else {
			return null;
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

	public boolean isLoop() {
		return src.component.equals(dest.component);
	}

	public void markActive() {
		active = true;
		if (activity.isEmpty()) {
			activity.add(new TimeFrame(Agenda.now()));
		} else {
			var last = activity.last();

			if (last.isClosed()) {
				activity.add(new TimeFrame(Agenda.now()));
			} else {
				last.end(Agenda.now());
			}
		}
	}

	public void markInactive() {
		this.active = false;
	}

}