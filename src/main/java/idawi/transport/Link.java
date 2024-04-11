package idawi.transport;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import idawi.Component;
import idawi.Idawi;
import idawi.service.local_view.Info;
import idawi.service.local_view.Infoable;
import toools.SizeOf;

public class Link implements Infoable, SizeOf {
	// the network interface on the neighbor side
	public TransportService src, dest;
	public Info info;
	public Activity activity;
	private boolean active = false;
	public long nbMsgs;
	public long traffic;
	public double latency;
	public boolean toBeResolved;

	public Link(TransportService from) {
		this(from, null);
		this.toBeResolved = true;
	}

	public Link(TransportService from, TransportService to) {
		Objects.requireNonNull(from);
		this.src = from;
		this.dest = to;
		info = new Info() {
			@Override
			public final boolean involves(Component d) {
				return src.component.equals(d) || dest.component.equals(d);
			}

			@Override
			public long sizeOf() {
				return super.sizeOf() + 32 + activity.sizeOf();
			}

			@Override
			public void exposeComponent(Predicate<Component> p) {
				p.test(dest.component);
			}
		};
	}

	@Override
	public long sizeOf() {
		return info.sizeOf() + 16 + activity.sizeOf() + 25;
	}

	public List<Link> impactedLinks() {
		var r = src.component.localView().g.findLinks(l -> l.src == src);
//		Cout.debug("*** " + this + " : " + r);
		return r;
	}

	public boolean headsTo(Component c) {
		return c.equals(dest.component);
	}

	@Override
	public String toString() {
		var t = getCommonTransport();

		if (t != null) {
			return src.component + " =" + src.getName() + "=> " + dest.component;
		} else {
			return src + " => " + dest;
		}
	}

	public Class<? extends TransportService> getCommonTransport() {
		if (src == null || dest == null || src.getClass() != dest.getClass()) {
			return null;
		} else {
			return src.getClass();
		}
	}

	public String getTransportName() {
		if (src.getClass() == dest.getClass()) {
			return src.getFriendlyName();
		} else {
			return src.getFriendlyName() + "/" + dest.getFriendlyName();
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

	public boolean isActive() {
		return active;// activity.available();
	}

	public void markActive() {
		active = true;
/*
		if (activity.isEmpty()) {
			activity.add(new TimeFrame(Idawi.agenda.now()));
		} else {
			var last = activity.last();

			if (last.isClosed()) {
				activity.add(new TimeFrame(Idawi.agenda.now()));
			} else {
				last.end(Idawi.agenda.now());
			}
		}
		*/
	}

	public void markInactive() {
		this.active = false;
	}

	public double latency() {
		return src.latency();
	}

	@Override
	public Info asInfo() {
		return info;
	}

}