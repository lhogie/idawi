package idawi.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
	public Set<SharedMemoryTransport> outs = new HashSet<>();

	public SharedMemoryTransport(Component c) {
		super(c);
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
	protected void multicastImpl(Message msg, Collection<OutNeighbor> throughSpecificNeighbors) {
		for (var n : throughSpecificNeighbors) {
			f(msg, n.dest);
		}
	}

	@Override
	protected void bcastImpl(Message msg) {
		outs.forEach(out -> f(msg, out));
	}

	private void f(Message msg, TransportService out) {
//		new Exception().printStackTrace();
		final var clone = msg.clone();
		
		Service.threadPool.submit(() -> {
			try {
				out.processIncomingMessage(clone);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}

	public void connectTo(Component dest) {
		var destNetworkInterface = dest.lookup(getClass());

		if (destNetworkInterface == null)
			throw new IllegalStateException("component " + dest + " does not have transport " + getClass());

		connectTo(destNetworkInterface);
	}

	public void connectTo(SharedMemoryTransport networkInterfaceOnDestination) {
		outs.add(networkInterfaceOnDestination);
		var dest = networkInterfaceOnDestination;
		component.digitalTwinService().add(dest.component);
		component.digitalTwinService().add(component.now(), this, 0, dest);
	}

	public void disconnectFrom(Component b) {
		var t = b.lookup(getClass());

		if (t == null)
			throw new IllegalStateException("component " + b + " does not have transport " + getClass());

		if (outs.contains(t))
			throw new IllegalStateException("component " + b + " is not out of " + component);

		outs.remove(t);
	}

	public boolean isConnectedTo(Component c) {
		return actualNeighbors().contains(c);
	}

	public static void chain(List<Component> l, Class<? extends SharedMemoryTransport> t) {
		int n = l.size();

		for (int i = 1; i < n; ++i) {
			var previous = l.get(i - 1).lookup(t);
			previous.connectTo(l.get(i));
		}
	}

	public static void chainRandomly(List<Component> l, int n, Random r, Class<? extends SharedMemoryTransport> t) {
		List<Component> l2 = new ArrayList<>(l);

		for (int i = 0; i < n; ++i) {
			Collections.shuffle(l2, r);
			chain(l2, t);
		}
	}

	public static void random(List<Component> l, int nbEdges, Class<? extends SharedMemoryTransport> t) {
		chain(l, t);
		nbEdges -= l.size() - 1;
		while (nbEdges > 0) {
			final var a = l.get(ThreadLocalRandom.current().nextInt(l.size()));
			final var ts = a.lookup(t);
			var b = a;

			while (b == a || ts.isConnectedTo(b)) {
				b = l.get(ThreadLocalRandom.current().nextInt(l.size()));
			}

			ts.connectTo(b);
		}
	}

	public static void gnp(List<Component> l, double p, Class<? extends SharedMemoryTransport> t) {
		for (var a : l) {
			for (var b : l) {
				if (a != b && Math.random() < p && !a.lookup(t).isConnectedTo(b)) {
					a.lookup(t).connectTo(b);
				}
			}
		}
	}

	public static void randomTree(List<Component> l, Class<? extends SharedMemoryTransport> t) {
		int n = l.size();

		for (int i = 1; i < n; ++i) {
			var from = l.get(i).lookup(t);
			var to = l.get(ThreadLocalRandom.current().nextInt(i));
			from.connectTo(to);
			to.lookup(t).connectTo(from);
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
					a.lookup(t).connectTo(b);
				}
			}
		}
	}

	public static void reconnectAccordingToDistance(ArrayList<Component> l) {
		// remove invalid links
		for (var a : l) {
			for (var ta : a.services(SharedMemoryTransport.class)) {
				for (var out : ta.outs) {
					if (distance(a, out.component) > ta.emissionRange) { // too far
						ta.disconnectFrom(out.component);
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
							var tb = b.lookup(ta.getClass());

							if (tb != null) {
								if (!ta.outs.contains(tb)) {
									ta.connectTo(tb);
								}
							}
						}
					}
				}
			}
		}
	}

	private static double distance(Component a, Component b) {
		return a.lookup(LocationService.class).getLocation()
				.distanceFrom(b.lookup(SimulatedLocationService.class).getLocation());
	}

	@Override
	public Collection<Component> actualNeighbors() {
		return outs.stream().map(t -> t.component).toList();
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

}
