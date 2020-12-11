package idawi;

public interface NeighborhoodListener
{
	void peerJoined(ComponentInfo peer, TransportLayer protocol);

	void peerLeft(ComponentInfo peer, TransportLayer protocol);
}
