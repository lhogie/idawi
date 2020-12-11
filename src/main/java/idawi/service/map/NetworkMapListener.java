package idawi.service.map;

import idawi.ComponentInfo;

public interface NetworkMapListener {
	void newNode(ComponentInfo p);
	void newEdge(ComponentInfo u, ComponentInfo v);
	void edgeRemoved(ComponentInfo u, ComponentInfo v);
}
