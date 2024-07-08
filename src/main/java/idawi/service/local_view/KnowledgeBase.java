package idawi.service.local_view;

import java.util.stream.Stream;

import idawi.Component;
import idawi.Service;

public abstract class KnowledgeBase extends Service {

	public KnowledgeBase(Component component) {
		super(component);
	}

	public double avgReliability(double now) {
		return infos().mapToDouble(i -> i.reliability(now)).average().getAsDouble();
	}

	@Override
	public long sizeOf() {
		return super.sizeOf() + infos().mapToLong(i -> 8 + i.sizeOf()).sum();
	}

	public abstract Stream<Info> infos();
}
