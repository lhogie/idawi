package idawi.knowledge_base.info;

import java.util.function.Consumer;

import idawi.knowledge_base.ComponentRef;
import idawi.transport.TransportService;

public class DirectedLink extends NetworkLink {
	public ComponentRef src, dest;

	public DirectedLink(double date, ComponentRef src, Class<? extends TransportService> protocol, ComponentRef dest) {
		super(date, protocol);

		if (src == null)
			throw new NullPointerException();

		if (dest == null)
			throw new NullPointerException();

		this.src = src;
		this.dest = dest;
	}

	@Override
	public boolean involves(ComponentRef d) {
		return d.equals(src) || d.equals(dest);
	}

	@Override
	public void forEachComponent(Consumer<ComponentRef> c) {
		c.accept(src);
		c.accept(dest);
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		var o = (DirectedLink) obj;
		return o.transport == transport && o.src.equals(src) && o.dest.equals(dest);
	}

	@Override
	public String toString() {
		return src + " ==" + transport + "==> " + dest;
	}

	public boolean matches(ComponentRef a, Class<? extends TransportService> protocol, ComponentRef b) {
		return (a == null && a.equals(src)) && (protocol == null || protocol.equals(transport))
				&& (b == null || b.equals(dest));
	}
}