package idawi.transport;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import idawi.Component;

public class SharedMemoryTransport extends TransportService implements Broadcastable{

	public static final Set<Component> s = new HashSet<>();

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
	public void bcast(byte[] msg) {
		var outLinks = s.stream().map(c -> component.localView().g.findLink(this, c.service(getClass()), true, null))
				.toList();
		fakeSend(msg, outLinks);
	}
}
