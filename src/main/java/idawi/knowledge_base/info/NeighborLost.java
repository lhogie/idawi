package idawi.knowledge_base.info;

import java.util.function.Predicate;

import idawi.Component;
import idawi.transport.TransportService;

public class NeighborLost extends NetworkLink {
	public TransportService  lostNeighbor;

	public NeighborLost(double date, TransportService c, TransportService neighbor) {
		super(date, c);
		this.lostNeighbor = neighbor;
	}

	@Override
	public void exposeComponent(Predicate<Component> p) {
		var b = p.test(transport.component) || p.test(lostNeighbor.component);
	}
}