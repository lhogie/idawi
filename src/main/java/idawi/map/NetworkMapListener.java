package idawi.map;

import idawi.ComponentDescriptor;

public interface NetworkMapListener {
	void newNode(ComponentDescriptor p);
	void newEdge(ComponentDescriptor u, ComponentDescriptor v);
	void edgeRemoved(ComponentDescriptor u, ComponentDescriptor v);
}
