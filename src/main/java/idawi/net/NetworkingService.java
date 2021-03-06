package idawi.net;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.EOT;
import idawi.Message;
import idawi.NeighborhoodListener;
import idawi.RemoteException;
import idawi.RouteEntry;
import idawi.Service;
import idawi.map.MapService;
import idawi.routing.RoutingService;
import idawi.service.ServiceManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import toools.io.Cout;
import toools.thread.Threads;
import toools.util.Date;

public class NetworkingService extends Service {
	static {
		AtomicBoolean nastyErrorShown = new AtomicBoolean(false);

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
				System.err.println("The bad fastutil error is back in " + NetworkingService.class);

				if (!nastyErrorShown.get()) {
					e.printStackTrace();
					nastyErrorShown.set(true);
				}
			}
		});
	}

	public final MultiTransport transport;
	public final ConcurrentHashMap<Long, Message> aliveMessages = new ConcurrentHashMap<>();
	public final LongSet alreadySentMsgs = new LongOpenHashSet();
	public final LongSet alreadyReceivedMsgs = new LongOpenHashSet();
	private final AtomicLong nbMsgReceived = new AtomicLong();
	public static boolean debug = false;

	public NetworkingService(Component t) {
		super(t);
		transport = new MultiTransport();
		transport.update(t.descriptor());
		transport.setNewMessageConsumer(messagesFromNetwork);
		var lmi = new LMI();
		lmi.peer_lmi.put(component.descriptor(), lmi);
		transport.addProtocol(lmi);

		transport.listeners.add(new NeighborhoodListener() {
			@Override
			public void peerJoined(ComponentDescriptor newPeer, TransportLayer protocol) {
				synchronized (aliveMessages) {
					for (Message msg : aliveMessages.values()) {
						send(msg, protocol, Set.of(newPeer));
					}
				}
			}

			@Override
			public void peerLeft(ComponentDescriptor p, TransportLayer neighborhood) {
			}
		});
	}

	private Collection<ComponentDescriptor> listProtocols() {
		return transport.neighbors();
	}

	public long getNbMessagesReceived() {
		return nbMsgReceived.get();
	}

	public final Consumer<Message> messagesFromNetwork = (msg) -> {
		nbMsgReceived.incrementAndGet();

		if (debug) {
			Cout.debug(component + " RECV " + msg);
		}

		msg.receptionDate = Date.time();

		for (var s : component.services()) {
			if (s instanceof MapService) {
				((MapService) s).feedWith(msg.route);
			}
		}

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

	private void notYetReceivedMsg(Message msg) {
		if (msg.to.isBroadcast()) {
			Service targetService = component.lookupService(msg.to.service);

			if (targetService != null) {
				targetService.considerNewMessage(msg);
			}
		} else {
			// if I'm and explicit recipient
			if (msg.to.getNotYetReachedExplicitRecipients().remove(component.descriptor())) {
				Service targetService = component.lookupService(msg.to.getServiceID());

				if (targetService != null) {
					targetService.considerNewMessage(msg);
				} else if (msg.requester != null) {
					send(new RemoteException("service not found: " + msg.to.getServiceID()), msg.requester);
					send(EOT.instance, msg.requester);
				}
			}
		}

		if (alreadySentMsgs.contains(msg.ID)) {
			// already sent
		} else if (msg.route.size() >= msg.to.getMaxDistance()) {
			// went far enough
		} else if (Math.random() > msg.to.getForwardProbability()) {
			// probabilistic drop
		} else if (!msg.to.isBroadcast() && msg.to.getNotYetReachedExplicitRecipients().isEmpty()) {
			// all explicit recipients have been reached
		} else {
			Collection<ComponentDescriptor> neighbors = neighbors();

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
				msg.to.getNotYetReachedExplicitRecipients()
						.retainAll(firstReceptionOfMsg.to.getNotYetReachedExplicitRecipients());
			}
		}
	}

	public void send(Message msg) {
		send(msg, transport);
	}

	public void send(Message msg, TransportLayer protocol) {
		Collection<ComponentDescriptor> relays = new HashSet<>();

		for (var s : component.services()) {
			if (s instanceof RoutingService) {
				RoutingService router = (RoutingService) s;
				relays.addAll(router.findRelaysToReach(protocol, msg.to.notYetReachedExplicitRecipients));
				// Cout.debug("ROUTING: " + msg.to.notYetReachedExplicitRecipients + " -> " +
				// relays);
			}
		}

		send(msg, protocol, relays);
	}

	public void send(Message msg, TransportLayer protocol, Collection<ComponentDescriptor> relays) {
		if (debug) {
			Cout.debugSuperVisible(
					component + " sends via transport " + protocol + " and relays " + relays + ", msg: " + msg);
		}

		if (relays.isEmpty()) {
			return;
		}

		alreadySentMsgs.add(msg.ID);

		// in order to avoid modifying the immutable set created by Set.of()
		if (msg.to.notYetReachedExplicitRecipients != null) {
			msg.to.notYetReachedExplicitRecipients = new HashSet<ComponentDescriptor>(
					msg.to.notYetReachedExplicitRecipients);
		}

		aliveMessages.put(msg.ID, msg);

		RouteEntry e = new RouteEntry();
		e.component = component.descriptor();
		e.emissionDate = Date.time();
		e.protocolName = protocol.getName();
		msg.route.add(e);
		// Cout.debug("sending: " + msg + " via " + protocol.getName());
		// Cout.debug(descriptor + " SEND " + protocol + " sending via " + relays + ": "
		// + msg);

		protocol.send(msg, relays);
	}

	public Collection<ComponentDescriptor> neighbors() {
		Collection<ComponentDescriptor> s = transport.neighbors();
		s.remove(component.descriptor());
		return s;
	}
}
