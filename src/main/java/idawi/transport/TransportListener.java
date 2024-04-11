package idawi.transport;

import idawi.messaging.Message;
import toools.io.Cout;

public interface TransportListener {
	void msgReceived(TransportService transportService, Message msg);

	void msgSent(TransportService transportService, Message msg, Iterable<Link> outLinks);

	public static class StdOut implements TransportListener {

		@Override
		public void msgReceived(TransportService transportService, Message msg) {
			Cout.debug(transportService + " receives " + msg);

		}

		@Override
		public void msgSent(TransportService transportService, Message msg, Iterable<Link> outLinks) {
			Cout.debug(transportService.component + " uses '" + transportService.getName() + "' to send: " + msg +  " via " + outLinks);
		}

	}
}
