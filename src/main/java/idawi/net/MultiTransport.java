package idawi.net;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Message;
import idawi.NeighborhoodListener;

public class MultiTransport extends TransportLayer {

	private final Set<TransportLayer> transports = new HashSet<>();
	public final Map<ComponentDescriptor, Set<TransportLayer>> peer2transports = new HashMap<>();
	private boolean run = false;

	public MultiTransport(Component c) {
		super(c);
	}

	public void addTransport(TransportLayer transport) {
		synchronized (transports) {
			transports.add(transport);
		}

		transport.setNewMessageConsumer(msg -> {
			if (!run) {
				return;
			}

			add(msg.route.last().component, transport);
			deliverToConsumer(msg);
		});

		transport.listeners.add(new NeighborhoodListener() {
			@Override
			public void neighborLeft(ComponentDescriptor peer, TransportLayer protocol) {
				listeners.forEach(p -> p.neighborLeft(peer, protocol));
			}

			@Override
			public void newNeighbor(ComponentDescriptor peer, TransportLayer protocol) {
				listeners.forEach(p -> p.newNeighbor(peer, protocol));
			}
		});
	}

	@Override
	public void send(Message msg, Collection<ComponentDescriptor> relays) {
//		Cout.debug("relays: " + relays);
//		new Exception().printStackTrace();
		if (!run) {
			return;
		}

		for (ComponentDescriptor relay : relays) {
			// use a random identified working protocol
			var identifiedTransports = peer2transports.get(relay);

			if (identifiedTransports != null && !identifiedTransports.isEmpty()) {
				var p = peer2transports.get(relay).iterator().next();
				msg.route.last().protocolName = p.getName();
//				System.out.println("***" + p.getClass());

				p.send(msg, Arrays.asList(relay));

			} else {
				// send through all protocols
				for (TransportLayer protocol : transports) {
					if (protocol.canContact(relay)) {
//						Cout.debug(":) ): : :) " + protocol.getName());
						msg.route.last().protocolName = protocol.getName();
						protocol.send(msg, Set.of(relay));
					}
				}
			}
		}
	}

	@Override
	public String getName() {
		StringBuilder b = new StringBuilder();
		b.append("multiprotocol/");
		transports.forEach(p -> b.append(p.getName() + "/"));
		return b.substring(0, b.length() - 1);
	}

	@Override
	public boolean canContact(ComponentDescriptor c) {
		for (TransportLayer p : transports) {
			if (p.canContact(c)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void injectLocalInfoTo(ComponentDescriptor c) {
		synchronized (transports) {
			transports.forEach(p -> p.injectLocalInfoTo(c));
		}
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
		Collection<ComponentDescriptor> peers = new HashSet<>();
		transports.forEach(p -> {
//			Cout.debug(p + " => " + p.neighbors());
			peers.addAll(p.neighbors());
		});
		return peers;
	}

	public Map<ComponentDescriptor, Set<TransportLayer>> neighbors2() {
		Map<ComponentDescriptor, Set<TransportLayer>> peer_protocols = new HashMap<>();

		for (TransportLayer protocol : transports) {
			for (ComponentDescriptor peer : protocol.neighbors()) {
				Set<TransportLayer> s = peer_protocols.get(peer);

				if (s == null) {
					peer_protocols.put(peer, s = new HashSet<>());
				}

				s.add(protocol);
			}
		}

		return peer_protocols;
	}

	public Set<TransportLayer> transports() {
		return transports;
	}
	
	public LMI lmi() {
		for (var t : transports) {
			if (t instanceof LMI) {
				return (LMI) t;
			}
		}

		return null;
	}

	public void update(Component c, ComponentDescriptor descriptor) {
		if (descriptor.udpPort != null) {
			UDPDriver udp = new UDPDriver(c);
			udp.setPort(descriptor.udpPort);
			addTransport(udp);
		}

		if (descriptor.tcpPort != null) {
			TCPDriver tcp = new TCPDriver(c);
			tcp.setPort(descriptor.tcpPort);
			addTransport(tcp);
		}
	}

	public void add(ComponentDescriptor c, TransportLayer t) {
		var usedTransports = peer2transports.get(c);

		if (usedTransports == null) {
			peer2transports.put(c, usedTransports = new HashSet<>());
		}

		usedTransports.add(t);
	}

}
