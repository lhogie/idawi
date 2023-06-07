package idawi.knowledge_base.info;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import idawi.Component;
import idawi.transport.TransportService;

public class HyperLink extends NetworkLink {
	public Set<TransportService> components = new HashSet<>();

	public HyperLink(double date, TransportService c, Set<TransportService> components) {
		super(date, c);
		this.components = components;
	}

	@Override
	public void exposeComponent(Predicate<Component> p) {
		for (var c : components) {
			if (p.test(c.component)) {
				return;
			}
		}
	}

}