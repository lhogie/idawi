package idawi.net;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.EOT;
import idawi.Message;
import idawi.NeighborhoodListener;
import idawi.Operation;
import idawi.RemoteException;
import idawi.RouteEntry;
import idawi.Service;
import idawi.TransportLayer;
import idawi.routing.RoutingService;
import idawi.service.registry.RegistryService;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import toools.thread.Threads;
import toools.util.Date;

public class NetworkingService extends Service {
	static {
		// delete deprecated messages
		Threads.newThread_loop(1000, () -> true, () -> {
			try {
				for (Component c : Component.componentsInThisJVM.values()) {
					NetworkingService t = c.lookupService(NetworkingService.class);
					for (Message m : t.aliveMessages.values()) {
						if (m.isExpired()) {
							t.alreadyReceivedMsgs.remove(m.ID);
							t.alreadySentMsgs.remove(m.ID);
							t.aliveMessages.remove(m.ID);
						}
					}
				}
			} catch (Throwable e) {
				System.err.println("The bad fastutil error is back in class " + NetworkingService.class);
			}
		});
	}

	public final MultiTransport transport;
	public final ConcurrentHashMap<Long, Message> aliveMessages = new ConcurrentHashMap<>();
	public final LongSet alreadySentMsgs = new LongOpenHashSet();
	public final LongSet alreadyReceivedMsgs = new LongOpenHashSet();

	public NetworkingService(Component t) {
		super(t);
		transport = new MultiTransport();
		transport.update(t.descriptor());
		transport.setNewMessageConsumer(messagesFromNetwork);
		transport.addProtocol(new LMI());

		transport.listeners.add(new NeighborhoodListener() {
			@Override
			public void peerJoined(ComponentInfo newPeer, TransportLayer protocol) {
				synchronized (aliveMessages) {
					for (Message msg : aliveMessages.values()) {
						send(msg, protocol, Set.of(newPeer));
					}
				}
			}

			@Override
			public void peerLeft(ComponentInfo p, TransportLayer neighborhood) {
			}
		});
	}

	@Operation
	private Collection<ComponentInfo> listProtocols() {
		return transport.neighbors();
	}

	public final Consumer<Message> messagesFromNetwork = (msg) -> {
		// Cout.debug(component + " RECV " + msg);
		msg.receptionDate = Date.time();
		learnFrom(msg);

		if (!msg.isExpired()) {
			// the message was already received
			if (alreadyReceivedMsgs.contains(msg.ID)) {
				alreadyReceivedMsg(msg);
			} else {
				alreadyReceivedMsgs.add(msg.ID);
				notYetReceivedMsg(msg);
			}
		}
	};

	private void learnFrom(Message msg) {
		msg.route.forEach(routeEntry -> Component.descriptorRegistry.update(routeEntry.component));

		for (Service s : component.services()) {
			if (s instanceof RoutingService) {
				((RoutingService) s).feedWith(msg.route);
			} else if (s instanceof RegistryService) {
				((RegistryService) s).feedWith(msg.route);
			}
		}
	}

	private void notYetReceivedMsg(Message msg) {
		if (msg.to.isBroadcast()) {
			Service targetService = component.lookupService(msg.to.service);

			if (targetService != null) {
				targetService.considerNewMessage(msg);
			}
		} else {
			// if I'm and explicit recipient
			if (msg.to.notYetReachedExplicitRecipients.remove(component.descriptor())) {
				Service targetService = component.lookupService(msg.to.service);

				if (targetService != null) {
					targetService.considerNewMessage(msg);
				} else if (msg.replyTo != null) {
					send(new RemoteException("service not found: " + msg.to.service), msg.replyTo, null);
					send(new EOT(), msg.replyTo, null);
				}
			}
		}

		if (alreadySentMsgs.contains(msg.ID)) {
			// already sent
		} else if (msg.route.size() >= msg.to.coverage) {
			// went far enough
		} else if (!msg.to.isBroadcast() && msg.to.notYetReachedExplicitRecipients.isEmpty()) {
			// all explicit recipients have been reached
		} else {
			Collection<ComponentInfo> neighbors = neighbors();

			if (neighbors.size() == 1 && neighbors.contains(msg.route.last())) {
				// don't resend to the guy who just sent it
			} else {
//			Cout.debugSuperVisible(peer + " forwarding " + msg);
				send(msg);
			}
		}
	}

	private void alreadyReceivedMsg(Message msg) {
		if (!msg.to.isBroadcast()) {
			Message firstReceptionOfMsg = aliveMessages.get(msg.ID);

			if (firstReceptionOfMsg == null) {
				aliveMessages.put(msg.ID, msg);
			} else {
				msg.to.notYetReachedExplicitRecipients
						.retainAll(firstReceptionOfMsg.to.notYetReachedExplicitRecipients);
			}
		}
	}

	public void send(Message msg) {
//		Cout.debugSuperVisible(component +  " sends " + msg);
		send(msg, transport);
	}

	public void send(Message msg, TransportLayer protocol) {
		for (var s : component.services()) {
			if (s instanceof RoutingService) {
				RoutingService router = (RoutingService) s;
				Collection<ComponentInfo> relays = router.findRelaysToReach(protocol,
						msg.to.notYetReachedExplicitRecipients);
				// Cout.debug("ROUTING: " + msg.to.peers + " -> " + relays);
				send(msg, protocol, relays);
				return;
			}
		}
	}

	public void send(Message msg, TransportLayer protocol, Collection<ComponentInfo> relays) {
		if (relays.isEmpty()) {
			return;
		}

		alreadySentMsgs.add(msg.ID);

		synchronized (aliveMessages) {
			// in order to avoid modifying the immutable set created by Set.of()
			if (msg.to.notYetReachedExplicitRecipients != null) {
				msg.to.notYetReachedExplicitRecipients = new HashSet<ComponentInfo>(
						msg.to.notYetReachedExplicitRecipients);
			}

			aliveMessages.put(msg.ID, msg);
		}

		RouteEntry e = new RouteEntry();
		e.component = component.descriptor();
		e.date = System.nanoTime();
		e.protocolName = protocol.getName();
		msg.route.add(e);
		msg.emissionDate = Date.time();
		// Cout.debug("sending: " + msg + " via " + protocol.getName());
		// Cout.debug(descriptor + " SEND " + protocol + " sending via " + relays + ": "
		// + msg);

		protocol.send(msg, relays);
	}

	public Collection<ComponentInfo> neighbors() {
		Collection<ComponentInfo> s = transport.neighbors();
		s.remove(component.descriptor());
		return s;
	}

	@Override
	public void inform(ComponentInfo p) {
		super.inform(p);
		transport.injectLocalInfoTo(p);
	}
}
