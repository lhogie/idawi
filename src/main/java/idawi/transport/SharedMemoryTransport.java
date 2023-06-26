package idawi.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import idawi.Component;
import idawi.Service;
import idawi.messaging.Message;
import idawi.service.LocationService;
import idawi.service.SimulatedLocationService;
import jdotgen.EdgeProps;
import jdotgen.GraphvizDriver;
import jdotgen.VertexProps;

public class SharedMemoryTransport extends TransportService {

	public double emissionRange = Double.MAX_VALUE;

	public SharedMemoryTransport() {
	}

	public SharedMemoryTransport(Component c) {
		super(c);

		c.localView().links().add(new Link(this, this)); // loopback
	}

	@Override
	public String getName() {
		return "shared mem";
	}

	@Override
	public boolean canContact(Component c) {
		return c != null;
	}

	@Override
	protected void sendImpl(Message msg) {
		var c = msg.clone();

		Service.threadPool.submit(() -> {
			try {
				c.route.last().link.dest.processIncomingMessage(c);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}

	public void inoutTo(Component c) {
		outTo(c);
		c.need(SharedMemoryTransport.class).outTo(this);
	}

	public void outTo(Component dest) {
		var destNetworkInterface = dest.need(getClass());

		if (destNetworkInterface == null)
			throw new IllegalStateException("component " + dest + " does not have transport " + getClass());

		outTo(destNetworkInterface);
	}

	public void outTo(SharedMemoryTransport networkInterfaceOnDestination) {
		component.localView().links().add(new Link(this, networkInterfaceOnDestination));
	}

	public void connectTo(SharedMemoryTransport a, boolean bidi) {
		outTo(a);

		if (bidi) {
			a.outTo(this);
		}
	}

	public void disconnectFrom(Component b) {
		var t = b.need(getClass());

		if (t == null)
			throw new IllegalStateException("component " + b + " does not have transport " + getClass());

		if (outLinks().search(t) == null)
			throw new IllegalStateException("component " + b + " is not out of " + component);

		outLinks().remove(t);
	}

	public static void chain(List<Component> l, Class<? extends SharedMemoryTransport> t, boolean bidi) {
		int n = l.size();

		for (int i = 1; i < n; ++i) {
			l.get(i - 1).need(t).connectTo(l.get(i).need(t), bidi);
		}
	}

	public static void chainRandomly(List<Component> l, int n, Random r, Class<? extends SharedMemoryTransport> t, boolean bidi) {
		List<Component> l2 = new ArrayList<>(l);

		for (int i = 0; i < n; ++i) {
			Collections.shuffle(l2, r);
			chain(l2, t, bidi);
		}
	}

	public static void random(List<Component> l, int nbEdges, Class<? extends SharedMemoryTransport> t, boolean bidi) {
		chain(l, t, bidi);
		nbEdges -= l.size() - 1;
		while (nbEdges > 0) {
			final var a = l.get(ThreadLocalRandom.current().nextInt(l.size()));
			final var ts = a.need(t);
			var b = a;

			while (b == a || ts.outLinks().contains(b)) {
				b = l.get(ThreadLocalRandom.current().nextInt(l.size()));
			}

			ts.connectTo(b.need(t), bidi);
		}
	}

	public static void gnp(List<Component> l, double p, Class<? extends SharedMemoryTransport> t) {
		for (var a : l) {
			for (var b : l) {
				if (a != b && Math.random() < p && !a.need(t).outLinks().contains(b)) {
					a.need(t).outTo(b);
				}
			}
		}
	}

	public static void randomTree(List<Component> l, Class<? extends SharedMemoryTransport> t) {
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

	public static void clique(Collection<Component> components, Class<? extends SharedMemoryTransport> t) {
		for (var a : components) {
			for (var b : components) {
				if (a != b) {
					a.need(t).outTo(b);
				}
			}
		}
	}

	public static void reconnectAccordingToDistance(ArrayList<Component> l) {
		// remove invalid links
		for (var a : l) {
			for (var ta : a.services(SharedMemoryTransport.class)) {
				for (var out : ta.outLinks().links()) {
					if (distance(a, out.dest.component) > ta.emissionRange) { // too far
						ta.disconnectFrom(out.dest.component);
					}
				}
			}
		}

		// create new links
		for (var a : l) {
			for (var b : l) {
				if (a != b) {
					final var d = distance(a, b);

					for (var ta : a.services(SharedMemoryTransport.class)) {

						if (d < ta.emissionRange) {
							var tb = b.need(ta.getClass());

							if (tb != null) {
								if (ta.component.localView().links().stream()
										.anyMatch(ll -> ll.src.equals(ta) && ll.dest.equals(tb))) {
									ta.outTo(tb);
								}
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

	public static GraphvizDriver toDot(List<Component> components) {
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
						for (var n : t.actualNeighbors()) {
							e.from = c;
							e.to = n;
							e.directed = true;
							e.label = t.getFriendlyName();
							f.f();
						}
					}
				}
			}
		};
	}

	@Override
	public Collection<Component> actualNeighbors() {
		return outLinks().destinations();
	}

}
