package idawi.knowledge_base;

import idawi.Component;
import idawi.transport.TransportService;

public interface DigitalTwinListener {
	void newComponent(Component p);

	void componentHasGone(Component a);

	void newInteraction(TransportService from, TransportService to);

	void interactionStopped(TransportService from, TransportService to);
}
