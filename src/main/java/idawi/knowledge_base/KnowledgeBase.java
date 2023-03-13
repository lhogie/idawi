package idawi.knowledge_base;

import java.util.function.Predicate;

import idawi.Component;
import idawi.Service;

public abstract class KnowledgeBase extends Service {

	public KnowledgeBase(Component component) {
		super(component);
	}

	public double avgReliability(double now) {
		class A {
			double sum;
			long count = 0;
		}

		var a = new A();

		forEachInfo(i -> {
			a.sum += i.reliability(now);
			++a.count;
			return false;
		});

		return a.sum / a.count;
	}

	protected abstract void forEachInfo(Predicate<Info> c);

	public abstract void removeOutdated(double now);
}
