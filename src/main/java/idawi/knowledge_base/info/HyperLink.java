package idawi.knowledge_base.info;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import idawi.knowledge_base.ComponentRef;
import idawi.transport.TransportService;

public class HyperLink extends NetworkLink {
	public Set<ComponentRef> components = new HashSet<>();

	public HyperLink(double date, Class<? extends TransportService> protocol, Set<ComponentRef> components) {
		super(date, protocol);
		this.components = components;
	}

	@Override
	public boolean involves(ComponentRef d) {
		return components.contains(d);
	}

	@Override
	public void forEachComponent(Consumer<ComponentRef> l) {
		components.forEach(c -> l.accept(c));
	}

}