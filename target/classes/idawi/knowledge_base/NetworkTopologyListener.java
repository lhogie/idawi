package idawi.knowledge_base;

import idawi.Component;
import idawi.knowledge_base.info.DirectedLink;

public interface NetworkTopologyListener {
	void newComponent(Component p);
	void componentHasGone(Component a);
	void newInteraction(DirectedLink l);
	void interactionStopped(DirectedLink l);
}
