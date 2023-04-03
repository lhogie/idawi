package idawi.knowledge_base.info;

import java.util.function.Predicate;

import idawi.Component;
import idawi.transport.OutNeighbor;
import idawi.transport.TransportService;

public class DirectedLink extends NetworkLink {
	public OutNeighbor  dest;

	public DirectedLink(double date, TransportService c, OutNeighbor dest) {
		super(date, c);


		if (dest == null)
			throw new NullPointerException();

		this.dest = dest;
	}

	@Override
	public void exposeComponent(Predicate<Component> p) {
		if (!p.test(transport.component)) {
			p.test(dest.transport.component);
		}
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		var o = (DirectedLink) obj;
		return o.transport == transport && o.transport.equals(transport) && o.dest.equals(dest);
	}

	@Override
	public String toString() {
		return transport + " ==" + dest + "==> " + dest;
	}
}