package idawi.transport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import idawi.Component;

public class SharedMemoryTransport extends TransportService implements Broadcastable {
	public final List<Component> bcastTargets = new ArrayList<Component>();

	public SharedMemoryTransport(Component c) {
		super(c);
	}

	@Override
	public String getName() {
		return "shared mem";
	}

	@Override
	public void dispose(Link l) {
		// l.activity.close();
	}

	@Override
	public double latency() {
		return 0;
	}

	@Override
	protected void multicast(byte[] msg, Collection<Link> outLinks) {
		sendToTwin(msg, outLinks);
	}

	@Override
	public void bcast(byte[] msgBytes) {
		bcastTargets.forEach(t -> {
			var l = component.localView().g.findLink(this, t.service(SharedMemoryTransport.class), true, null);
			sendToTwin(msgBytes, l);
		});
	}

}
