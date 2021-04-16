package idawi.net;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import idawi.ComponentDescriptor;
import idawi.Message;
import idawi.NeighborhoodListener;
import toools.io.Cout;

public class MultiTransport extends TransportLayer {
	private final Map<String, TransportLayer> name2protocol = new HashMap<>();
	public final Map<ComponentDescriptor, TransportLayer> peer2protocol = new HashMap<>();
	private boolean run = false;

	public void addProtocol(TransportLayer p) {
		name2protocol.put(p.getName(), p);

		synchronized (name2protocol) {
			name2protocol.put(p.getName(), p);
		}

		p.setNewMessageConsumer(msg -> {
			if (!run) {
				return;
			}

			if (!peer2protocol.containsKey(msg.route.last().component)) {
				TransportLayer pp = name2protocol.get(msg.route.last().protocolName);
				if (pp != null) {
					peer2protocol.put(msg.route.last().component, pp);
				}
			}

			processIncomingMessage(msg);
		});

		p.listeners.add(new NeighborhoodListener() {
			@Override
			public void peerLeft(ComponentDescriptor peer, TransportLayer protocol) {
				listeners.forEach(p -> p.peerLeft(peer, protocol));
			}

			@Override
			public void peerJoined(ComponentDescriptor peer, TransportLayer protocol) {
				listeners.forEach(p -> p.peerJoined(peer, protocol));
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
			// if a working protocol has been identified, use it
			TransportLayer p = peer2protocol.get(relay);

			if (p != null) {
//				Cout.debug("using " + p);
				msg.route.last().protocolName = p.getName();
				p.send(msg, Arrays.asList(relay));
			} else {
				// send through all protocols
				for (TransportLayer protocol : name2protocol.values()) {
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
		name2protocol.values().forEach(p -> b.append(p.getName() + "/"));
		return b.substring(0, b.length() - 1);
	}

	@Override
	public boolean canContact(ComponentDescriptor c) {
		for (TransportLayer p : name2protocol.values()) {
			if (p.canContact(c)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void injectLocalInfoTo(ComponentDescriptor c) {
		synchronized (name2protocol) {
			name2protocol.values().forEach(p -> p.injectLocalInfoTo(c));
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
		name2protocol.values().forEach(p -> peers.addAll(p.neighbors()));
		return peers;
	}

	public Map<ComponentDescriptor, Set<TransportLayer>> neighbors2() {
		Map<ComponentDescriptor, Set<TransportLayer>> peer_protocols = new HashMap<>();

		for (TransportLayer protocol : name2protocol.values()) {
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

	public LMI lmi() {
		return (LMI) name2protocol.get("LMI");
	}

	public void update(ComponentDescriptor descriptor) {
		if (descriptor.udpPort != null) {
			UDPDriver udp = new UDPDriver();
			udp.setPort(descriptor.udpPort);
			addProtocol(udp);
		}

		if (descriptor.tcpPort != null) {
			TCPDriver tcp = new TCPDriver();
			tcp.setPort(descriptor.tcpPort);
			addProtocol(tcp);
		}
	}

}
