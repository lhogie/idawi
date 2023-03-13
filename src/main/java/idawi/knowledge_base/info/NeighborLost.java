package idawi.knowledge_base.info;

import java.util.function.Consumer;

import idawi.knowledge_base.ComponentRef;
import idawi.transport.TransportService;

public class NeighborLost extends NetworkLink {
	public ComponentRef c, lostNeighbor;

	public NeighborLost(double date, Class<? extends TransportService> protocol, ComponentRef c, ComponentRef neighbor) {
		super(date, protocol);
		this.c = c;
		this.lostNeighbor = neighbor;
	}

	@Override
	public boolean involves(ComponentRef d) {
		return d.equals(c) || d.equals(lostNeighbor);
	}

	@Override
	public void forEachComponent(Consumer<ComponentRef> c) {
		c.accept(this.c);
		c.accept(lostNeighbor);
	}
}