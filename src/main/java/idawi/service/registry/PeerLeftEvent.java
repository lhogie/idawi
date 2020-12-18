package idawi.service.registry;

import idawi.ComponentInfo;

public class PeerLeftEvent extends PeerEvent {
	public PeerLeftEvent(ComponentInfo p, String protocol) {
		super(p, protocol);
	}

	@Override
	public String toString() {
		return peer + " left from " + protocol;
	}
}
