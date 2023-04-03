package idawi.transport;

import java.util.function.Predicate;

import idawi.Component;
import idawi.knowledge_base.Info;

public class OutNeighbor extends Info {
	public TransportService transport;
	public double duration;

	@Override
	public void exposeComponent(Predicate<Component> p) {
		p.test(transport.component);
	}
}