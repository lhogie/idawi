package idawi.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Message;
import idawi.service.ServiceManager;
import toools.io.Cout;

public class LMI extends TransportLayer {

	public static final ExecutorService executorService = Executors.newSingleThreadExecutor();
	public final Map<ComponentDescriptor, LMI> peer2lmi = new HashMap<>();

	private boolean run;

	public LMI(Component c) {
		super(c);
	}

	@Override
	synchronized public void send(Message msg, Collection<ComponentDescriptor> neighborRelays) {
		if (!run) {
			return;
		}

		for (ComponentDescriptor n : neighborRelays) {
			LMI lmi = peer2lmi.get(n);

			if (lmi != null && lmi.run) {
				// simulate transport over the network
				Message clone = (Message) serializer.clone(msg);

				if (false) {
					lmi.processIncomingMessage(clone);
				} else {
					if (!executorService.isShutdown()) {
						executorService.submit(() -> {
							try {
								lmi.processIncomingMessage(clone);
							}
							catch(Throwable e) {
								e.printStackTrace();
							}
						});
					}
				}
			}
		}
	}

	@Override
	public String getName() {
		return "LMI";
	}

	@Override
	public boolean canContact(ComponentDescriptor c) {
		return c.name != null;
	}

	@Override
	public void injectLocalInfoTo(ComponentDescriptor c) {
	}

	@Override
	protected void start() {
		run = true;
	}

	@Override
	protected void stop() {
		run = false;
	}

	@Override
	public Collection<ComponentDescriptor> neighbors() {
		return peer2lmi.keySet();
	}

	public static void connect(Component a, Component b) {
		LMI almi = a.lookup(NetworkingService.class).transport.lmi();
		LMI blmi = b.lookup(NetworkingService.class).transport.lmi();
		blmi.peer2lmi.put(a.descriptor(), almi);
		almi.peer2lmi.put(b.descriptor(), blmi);
	}

	public static boolean areConnected(Component src, Component dest) {
		return src.lookup(NetworkingService.class).transport.lmi().peer2lmi.containsKey(dest.descriptor());
	}

	public static void chain(List<Component> l) {
		int n = l.size();

		for (int i = 1; i < n; ++i) {
			connect(l.get(i - 1), l.get(i));
		}
	}

	public static void chain(List<Component> l, int n, Random r) {
		List<Component> l2 = new ArrayList<>(l);

		for (int i = 0; i < n; ++i) {
			Collections.shuffle(l2, r);
			chain(l2);
		}
	}

	public static void random(List<Component> l, int nbEdges) {
		chain(l);
		nbEdges -= l.size() - 1;

		while (nbEdges > 0) {
			var a = l.get(ThreadLocalRandom.current().nextInt(l.size()));
			var b = a;

			while (b == a || areConnected(a, b)) {
				b = l.get(ThreadLocalRandom.current().nextInt(l.size()));
			}

			connect(a, b);
		}
	}
	
	public static void gnp(List<Component> l, double p) {
		for (var a : l) {
			for (var b : l) {
				if( a != b && ! areConnected(a, b) && Math.random() < p) {
					connect(a, b);
				}
			}
		}
	}

	public static void randomTree(List<Component> l) {
		int n = l.size();

		for (int i = 1; i < n; ++i) {
			connect(l.get(i), l.get(ThreadLocalRandom.current().nextInt(i)));
		}
	}

	public static List<Component> pick(List<Component> l, int degree, Random r) {
		List<Component> out = new ArrayList<>();

		while (out.size() < degree) {
			Component t = l.get(r.nextInt(l.size()));
			out.add(t);
		}

		return out;
	}

	public static void clique(Collection<Component> components) {
		for (var a : components) {
			for (var b : components) {
				if (a != b) {
					connect(a, b);
				}
			}
		}
	}
}
