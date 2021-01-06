package idawi;

import idawi.net.TransportLayer;

public interface NeighborhoodListener
{
	void peerJoined(ComponentDescriptor peer, TransportLayer protocol);

	void peerLeft(ComponentDescriptor peer, TransportLayer protocol);
}
