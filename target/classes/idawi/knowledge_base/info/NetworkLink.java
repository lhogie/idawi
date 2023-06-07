package idawi.knowledge_base.info;

import idawi.knowledge_base.Info;
import idawi.transport.TransportService;

public abstract class NetworkLink extends Info {
	public TransportService transport;
	double latency = -1;
	int throughput = -1;

	public NetworkLink(double date, TransportService c) {
		super(date);

		if (c == null)
			throw new NullPointerException();

		this.transport = c;
	}

}