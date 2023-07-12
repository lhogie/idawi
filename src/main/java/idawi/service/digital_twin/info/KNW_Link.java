package idawi.service.digital_twin.info;

import java.util.function.Predicate;

import idawi.Component;
import idawi.service.local_view.Info;
import idawi.transport.Link;
import idawi.transport.TransportService;

public class KNW_Link extends Info {
	public TransportService from;
	public TransportService to;

	public KNW_Link(double date, Link l) {
		super(date);
		this.from = l.src;
		this.to = l.dest;
	}

	@Override
	public void exposeComponent(Predicate<Component> p) {
		var b = p.test(from.component) || p.test(to.component);
	}
}