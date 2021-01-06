package idawi.net;

import java.util.ArrayList;
import java.util.List;

import idawi.ComponentDescriptor;
import idawi.NeighborhoodListener;

public abstract class Neighborhood {
	public final List<NeighborhoodListener> listeners = new ArrayList<>();

	public abstract void messageJustReceivedFrom(ComponentDescriptor peer);

	public abstract void seenDead(ComponentDescriptor peer);

}