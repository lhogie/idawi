package idawi.map;

import idawi.ComponentDescriptor;

public class PeerLeftEvent extends PeerEvent {
	public PeerLeftEvent(ComponentDescriptor p, String protocol) {
		super(p, protocol);
	}

	@Override
	public String toString() {
		return peer + " left from " + protocol;
	}
}
