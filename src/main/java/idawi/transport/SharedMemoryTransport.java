package idawi.transport;

import java.util.Collection;

import idawi.Component;
import idawi.messaging.Message;

public class SharedMemoryTransport extends TransportService {

	public SharedMemoryTransport(Component c) {
		super(c);
	}

	@Override
	public String getName() {
		return "shared mem";
	}

	@Override
	public void dispose(Link l) {
//		l.activity.close();
	}

	@Override
	public double latency() {
		return 0;
	}

	@Override
	protected void multicast(byte[] msg, Collection<Link> outLinks) {
//		Cout.debug(outLinks);
//		outLinks.forEach(l -> l.dest.processIncomingMessage((Message) l.dest.serializer.fromBytes(msg)));
		fakeSend(msg, outLinks);
	}

	@Override
	protected void bcast(byte[] msg) {
		multicast(msg, activeOutLinks());
	}
}
