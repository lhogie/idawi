package idawi.net;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import idawi.ComponentDescriptor;
import toools.thread.Threads;

public class MessageBuiltNeighborhood {
	private final Map<ComponentDescriptor, Long> name_lastSeenDate = new HashMap<>();
	public long timeOutMs = 5000;
	private final TransportLayer protocol;

	public MessageBuiltNeighborhood(TransportLayer protocol) {
		this.protocol = protocol;
		Threads.newThread_loop(1, () -> true, () -> removeOutDated());
	}

	public synchronized void removeOutDated() {
		Iterator<Entry<ComponentDescriptor, Long>> i = name_lastSeenDate.entrySet().iterator();

		while (i.hasNext()) {
			Entry<ComponentDescriptor, Long> e = i.next();
			ComponentDescriptor p = e.getKey();
			long lastSeen = e.getValue();

			// the peer has not been seen for a while
			if (System.currentTimeMillis() - lastSeen > timeOutMs) {
				// drop it
				i.remove();
				protocol.listeners.forEach(l -> l.peerLeft(p, protocol));
			}
		}
	}

	public synchronized void messageJustReceivedFrom(ComponentDescriptor peer) {
		boolean alreadyKnown = name_lastSeenDate.containsKey(peer);
		name_lastSeenDate.put(peer, System.currentTimeMillis());

		if ( ! alreadyKnown) {
			protocol.listeners.forEach(l -> l.peerJoined(peer, protocol));
		}
	}

	public synchronized void remove(ComponentDescriptor peer) {
		if (name_lastSeenDate.remove(peer) != null) {
		}
	}

	public Set<ComponentDescriptor> peers() {
		return name_lastSeenDate.keySet();
	}
}