package idawi.routing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import idawi.Component;
import idawi.transport.TransportService;

public class Route implements Serializable {
	private static final long serialVersionUID = 1L;

	public Emission initialEmission;

	public void add(Reception r) {
		if (isEmpty())
			throw new IllegalStateException("can't start with a reception");

		var e = ((Emission) last());
		e.subsequentReception = r;
		r.previousEmission = e;
	}

	public void add(Emission e) {
		if (initialEmission == null) {
			initialEmission = e;
			e.previousReception = e.subsequentReception = null;
		} else {
			var r = ((Reception) last());
			r.forward = e;
			e.previousReception = r;
		}
	}

	public List<Emission> emissions() {
		var r = new ArrayList<Emission>();
		var e = initialEmission;

		while (e != null) {
			r.add(e);
			e = e.nextEmission();
		}

		return r;
	}

	public List<Reception> receptions() {
		return emissions().stream().filter(e -> e.subsequentReception != null).map(e -> e.subsequentReception).toList();
	}

	public List<Component> components() {
		var components = new ArrayList<Component>();
		components.add(initialEmission.transport.component);
		Reception r = initialEmission.subsequentReception;

		while (r != null) {
			components.add(r.transport.component);
			r = r.nextReception();
		}

		return components;
	}

	public List<TransportService> transports() {
		return emissions().stream().map(e -> e.transport()).toList();
	}

	public Emission initialEmission() {
		return initialEmission;
	}

	public RouteEvent last() {
		RouteEvent e = initialEmission;

		while (e != null && e.hasNext()) {
			e = e.next();
		}

		return e;
	}

	public Reception lastReception() {
		var last = last();

		if (last instanceof Reception) {
			return (Reception) last;
		} else {
			return ((Emission) last).previousReception;
		}
	}

	public Emission lastEmission() {
		if (initialEmission == null) {
			return null;
		}

		var last = last();
		return last instanceof Emission ? (Emission) last : ((Reception) last).previousEmission;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(nbEvents() + " event(s), ");
		int i = 0;
		RouteEvent e = initialEmission;

		while (e != null) {
			b.append("e " + i++ + ": " + e);

			if (e.hasNext()) {
				b.append(", ");
			}

			e = e.next();
		}

		return b.toString();
	}

	@Override
	public boolean equals(Object o) {
		var r = (Route) o;
		RouteEvent e = initialEmission;
		RouteEvent oe = r.initialEmission;

		while (true) {
			if (e == null && oe == null) {
				return true;
			} else if (e == null || oe == null) {
				return false;
			} else if (!o.equals(oe)) {
				return false;
			}

			e = e.next();
			oe = oe.next();
		}
	}

	public int nbEvents() {
		return initialEmission == null ? 0 : 1 + initialEmission.remaining();
	}

	public double duration() {
		return last().date() - initialEmission().date();
	}

	public Iterable<RouteEvent> events() {
		return () -> new Iterator<RouteEvent>() {
			RouteEvent e = initialEmission;

			@Override
			public boolean hasNext() {
				return e != null;
			}

			@Override
			public RouteEvent next() {
				var r = e;
				e = e.next();
				return r;
			}
		};
	}

	public boolean isEmpty() {
		return nbEvents() == 0;
	}

	public void removeLast() {
		if (nbEvents() == 1) {
			this.initialEmission = null;
		} else {
			lastEmission().previousReception.forward = null;
		}
	}
}
