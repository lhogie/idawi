package idawi;

import idawi.net.TransportLayer;

public interface NeighborhoodListener
{
	void newNeighbor(ComponentDescriptor peer, TransportLayer protocol);

	void neighborLeft(ComponentDescriptor peer, TransportLayer protocol);
}
