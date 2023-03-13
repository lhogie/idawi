package idawi.knowledge_base.info;

import idawi.knowledge_base.Info;
import idawi.transport.TransportService;

public abstract class NetworkLink extends Info {
	public Class<? extends TransportService> transport;
	double latency = -1;
	int throughput = -1;

	public NetworkLink(double date, Class<? extends TransportService> transport) {
		super(date);

		if (transport == null)
			throw new NullPointerException();

		this.transport = transport;
	}

}