package idawi.service.map;

import java.io.Serializable;

import idawi.ComponentInfo;
import idawi.TransportLayer;

public class PeerEvent implements Serializable {
	final public ComponentInfo peer;
	final public String protocol; 

	public PeerEvent(ComponentInfo p, String protocol) {
		this.peer = p;
		this.protocol = protocol;
	}
}
