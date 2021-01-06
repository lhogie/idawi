package idawi.map;

import idawi.ComponentDescriptor;

public class PeerJoinedEvent extends PeerEvent {
	public PeerJoinedEvent(ComponentDescriptor p, String protocol) {
		super(p, protocol);
	}

	@Override
	public String toString() {
		return peer + " joined by " + protocol;
	}
}
