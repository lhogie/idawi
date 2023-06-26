package idawi.service.local_view;

import idawi.Component;
import idawi.transport.TransportService;

public interface NetworkTopologyListener {
	void newComponent(Component p);

	void componentHasGone(Component a);

	void newInteraction(TransportService from, TransportService to);

	void interactionStopped(TransportService from, TransportService to);
}
