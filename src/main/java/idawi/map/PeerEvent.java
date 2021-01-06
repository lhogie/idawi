package idawi.map;

import java.io.Serializable;

import idawi.ComponentDescriptor;

public class PeerEvent implements Serializable {
	final public ComponentDescriptor peer;
	final public String protocol; 

	public PeerEvent(ComponentDescriptor p, String protocol) {
		this.peer = p;
		this.protocol = protocol;
	}
}
