package idawi.routing;

import idawi.messaging.Message;
import idawi.transport.TransportService;

public interface TrafficListener {
	void newMessageReceived(TransportService t, Message msg);

}
