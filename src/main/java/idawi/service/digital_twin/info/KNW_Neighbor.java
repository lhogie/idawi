package idawi.service.digital_twin.info;

import java.util.function.Predicate;

import idawi.Component;
import idawi.service.local_view.Info;
import idawi.transport.Link;
import idawi.transport.TransportService;

public class KNW_Neighbor extends Info {
	public TransportService c;
	public Link newOutNeighbor;

	public KNW_Neighbor(double date, TransportService c, Link l) {
		super(date);
		this.c = c;
		this.newOutNeighbor = l;
	}

	@Override
	public void exposeComponent(Predicate<Component> p) {
		var b = p.test(c.component) || p.test(newOutNeighbor.dest.component);
	}
}