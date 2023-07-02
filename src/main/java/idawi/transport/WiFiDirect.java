package idawi.transport;

import idawi.Component;
import idawi.messaging.Message;
import toools.exceptions.NotYetImplementedException;

public class WiFiDirect extends TransportService {

	public WiFiDirect(Component c) {
		super(c);
	}
	
	@Override
	public String getName() {
		return "Wi-Fi";
	}

	@Override
	public boolean canContact(Component c) {
		return true;
	}

	@Override
	protected void sendImpl(Message msg) {
		throw new NotYetImplementedException();
	}

	@Override
	public void dispose(Link l) {
	}
}
