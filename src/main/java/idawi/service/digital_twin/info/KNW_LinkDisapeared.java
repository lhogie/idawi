package idawi.service.digital_twin.info;

import java.util.function.Predicate;

import idawi.Component;
import idawi.service.local_view.Info;
import idawi.transport.Link;
import idawi.transport.TransportService;

public class KNW_LinkDisapeared extends Info {
	
	public TransportService from;
	public TransportService to;

	public KNW_LinkDisapeared(double date, TransportService c, TransportService neighbor) {
		super(date);
		this.from = c;
		this.to = neighbor;
	}


	@Override
	public void exposeComponent(Predicate<Component> p) {
		var b = p.test(from.component) || p.test(to.component);
	}
}