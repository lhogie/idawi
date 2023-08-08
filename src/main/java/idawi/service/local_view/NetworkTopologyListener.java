package idawi.service.local_view;

import idawi.Component;
import idawi.transport.Link;

public interface NetworkTopologyListener {
	void newComponent(Component p);

	void componentHasGone(Component a);

	void linkActivated(Link l);

	void linkDeactivated(Link l);
}
