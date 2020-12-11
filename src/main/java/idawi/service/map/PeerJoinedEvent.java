package idawi.service.map;

import idawi.ComponentInfo;

public class PeerJoinedEvent extends PeerEvent {
	public PeerJoinedEvent(ComponentInfo p, String protocol) {
		super(p, protocol);
	}
	
	@Override
	public String toString()
	{
		return peer + " joined by " + protocol;
	}
}
