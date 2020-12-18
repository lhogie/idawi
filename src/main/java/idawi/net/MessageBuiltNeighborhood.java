package idawi.net;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import idawi.ComponentInfo;
import idawi.TransportLayer;
import toools.thread.Threads;

public class MessageBuiltNeighborhood {
	private final Map<ComponentInfo, Long> name_lastSeenDate = new HashMap<>();
	public long timeOutMs = 5000;
	private final TransportLayer protocol;

	public MessageBuiltNeighborhood(TransportLayer protocol) {
		this.protocol = protocol;
		Threads.newThread_loop(1, () -> true, () -> removeOutDated());
	}

	public synchronized void removeOutDated() {
		Iterator<Entry<ComponentInfo, Long>> i = name_lastSeenDate.entrySet().iterator();

		while (i.hasNext()) {
			Entry<ComponentInfo, Long> e = i.next();
			ComponentInfo p = e.getKey();
			long lastSeen = e.getValue();

			// the peer has not been seen for a while
			if (System.currentTimeMillis() - lastSeen > timeOutMs) {
				// drop it
				i.remove();
				protocol.listeners.forEach(l -> l.peerLeft(p, protocol));
			}
		}
	}

	public synchronized void messageJustReceivedFrom(ComponentInfo peer) {
		boolean alreadyKnown = name_lastSeenDate.containsKey(peer);
		name_lastSeenDate.put(peer, System.currentTimeMillis());

		if ( ! alreadyKnown) {
			protocol.listeners.forEach(l -> l.peerJoined(peer, protocol));
		}
	}

	public synchronized void remove(ComponentInfo peer) {
		if (name_lastSeenDate.remove(peer) != null) {
		}
	}

	public Set<ComponentInfo> peers() {
		return name_lastSeenDate.keySet();
	}
}