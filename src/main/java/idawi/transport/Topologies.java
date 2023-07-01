package idawi.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import idawi.Component;
import idawi.service.LocationService;
import idawi.service.SimulatedLocationService;
import jdotgen.EdgeProps;
import jdotgen.GraphvizDriver;
import jdotgen.VertexProps;
import jdotgen.GraphvizDriver.Validator;

public class Topologies {

	public static void chain(List<Component> l, Class<? extends TransportService> t, boolean bidi) {
		int n = l.size();

		for (int i = 1; i < n; ++i) {
			l.get(i - 1).need(t).connectTo(l.get(i).need(t), bidi);
		}
	}

	public static void chainRandomly(List<Component> l, int n, Random r, Class<? extends TransportService> t,
			boolean bidi) {
		List<Component> l2 = new ArrayList<>(l);

		for (int i = 0; i < n; ++i) {
			Collections.shuffle(l2, r);
			chain(l2, t, bidi);
		}
	}

	public static void random(List<Component> l, int nbEdges, Class<? extends TransportService> t, boolean bidi) {
		chain(l, t, bidi);
		nbEdges -= l.size() - 1;

		while (nbEdges > 0) {
			final var a = l.get(ThreadLocalRandom.current().nextInt(l.size()));
			final var at = a.need(t);
			var b = a;
			var outN = at.outLinks().map(sl -> sl.dest.component).toList();

			while (b == a || outN.contains(b)) {
				b = l.get(ThreadLocalRandom.current().nextInt(l.size()));
			}

			at.connectTo(b.need(t), bidi);
		}
	}

	public static void gnp(List<Component> l, double p, Class<? extends TransportService> t) {
		for (var a : l) {
			for (var b : l) {
				if (a != b && Math.random() < p
						&& !a.need(t).outLinks().map(ol -> ol.dest.component).anyMatch(c -> c.equals(b))) {
					a.need(t).outTo(b);
				}
			}
		}
	}

	public static void randomTree(List<Component> l, Class<? extends TransportService> t) {
		int n = l.size();

		for (int i = 1; i < n; ++i) {
			var from = l.get(i).need(t);
			var to = l.get(ThreadLocalRandom.current().nextInt(i));
			from.outTo(to);
			to.need(t).outTo(from);
		}
	}

	public static List<Component> pick(List<Component> l, int n, Random r) {
		List<Component> out = new ArrayList<>();

		while (out.size() < n) {
			Component t = l.get(r.nextInt(l.size()));
			out.add(t);
		}

		return out;
	}

	public static void clique(Collection<Component> components, Class<? extends TransportService> t) {
		for (var a : components) {
			for (var b : components) {
				if (a != b) {
					a.need(t).outTo(b);
				}
			}
		}
	}

	public static void reconnectAccordingToDistance(ArrayList<Component> components) {
		// remove invalid links
		for (var c : components) {
			var ta = c.need(SharedMemoryTransport.class);
			var linksToKill = c.localView().links().stream()
					.filter(l -> l.matches(ta, null) && distance(c, l.dest.component) > ta.emissionRange)
					.toList();
			//ta.
		}

		// create new links
		for (var a : components) {
			for (var b : components) {
				if (a != b) {
					final var d = distance(a, b);
					var ta = a.need(SharedMemoryTransport.class);

					if (d < ta.emissionRange) {
						var tb = b.need(ta.getClass());

						if (tb != null) {
							if (ta.component.localView().links().stream().noneMatch(ll -> ll.matches(ta, tb))) {
								ta.outTo(tb);
							}
						}
					}
				}
			}
		}
	}

	private static double distance(Component a, Component b) {
		return a.need(LocationService.class).getLocation()
				.distanceFrom(b.need(SimulatedLocationService.class).getLocation());
	}

	public static GraphvizDriver toDot(Collection<Component> components) {
		return new GraphvizDriver() {

			@Override
			protected void findVertices(VertexProps v, Validator f) {
				for (var c : components) {
					v.id = c;
					f.f();
				}
			}

			@Override
			protected void findEdges(EdgeProps e, Validator f) {
				for (var c : components) {
					for (var t : c.services(TransportService.class)) {
						t.outLinks().map(l -> l.dest.component).forEach(n -> {
							e.from = c;
							e.to = n;
							e.directed = true;
							e.label = t.getFriendlyName();
							f.f();
						});
					}
				}
			}
		};
	}

}
