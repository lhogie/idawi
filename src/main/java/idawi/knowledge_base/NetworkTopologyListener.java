package idawi.knowledge_base;

import idawi.knowledge_base.info.DirectedLink;

public interface NetworkTopologyListener {
	void newComponent(ComponentRef p);
	void componentHasGone(ComponentRef a);
	void newInteraction(DirectedLink l);
	void interactionStopped(DirectedLink l);
}
