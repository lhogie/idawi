package idawi.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import idawi.ComponentInfo;
import idawi.NeighborhoodListener;

public abstract class Neighborhood {
	public final List<NeighborhoodListener> listeners = new ArrayList<>();

	public abstract void messageJustReceivedFrom(ComponentInfo peer);

	public abstract void seenDead(ComponentInfo peer);

}