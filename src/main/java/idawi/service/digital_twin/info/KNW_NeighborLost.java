package idawi.service.digital_twin.info;

import java.util.function.Predicate;

import idawi.Component;
import idawi.service.local_view.Info;
import idawi.transport.TransportService;

public class KNW_NeighborLost extends Info {
	
	public TransportService from;
	public TransportService lostNeighbor;

	public KNW_NeighborLost(double date, TransportService c, TransportService neighbor) {
		super(date);
		this.from = c;
		this.lostNeighbor = neighbor;
	}

	@Override
	public void exposeComponent(Predicate<Component> p) {
		var b = p.test(from.component) || p.test(lostNeighbor.component);
	}
}