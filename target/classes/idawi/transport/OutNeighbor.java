package idawi.transport;

import java.util.function.Predicate;

import idawi.Component;
import idawi.knowledge_base.Info;

public class OutNeighbor extends Info {
	public TransportService dest;
	public double duration;

	@Override
	public void exposeComponent(Predicate<Component> p) {
		p.test(dest.component);
	}

	@Override
	public String toString() {
		return "---" + dest.getName() + "--->" + dest.component + " (" + duration + "s)";
	}
}