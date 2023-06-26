package idawi.service.local_view;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import toools.SizeOf;

public abstract class KnowledgeBase extends Service implements Consumer<Info>, SizeOf {

	public KnowledgeBase(Component component) {
		super(component);
	}

	public double avgReliability(double now) {
		return infos().mapToDouble(i -> i.reliability(now)).average().getAsDouble();
	}

	public abstract void removeOutdated(double now);


	public class consider extends TypedInnerClassEndpoint {
		@Override
		public String getDescription() {
			return "considers a new info";
		}

		public void f(Collection<Info> newInfos) {
			newInfos.forEach(k -> accept(k));
		}
	}


	public class size extends TypedInnerClassEndpoint {
		public long size() {
			return sizeOf();
		}

		@Override
		public String getDescription() {
			return "nb of bytes used by this knowkedge base";
		}
	}

	public void disseminate(Set<Info> i) {
		component.defaultRoutingProtocol().exec(LocalViewService.class, consider.class, i);
	}

	public void disseminate() {
		infos().filter(i -> Math.random() < i.reliability(component.now())).forEach(i -> disseminate(Set.of(i)));
	}

	@Override
	public long sizeOf() {
		return infos().mapToLong(i -> 8 + i.sizeOf()).sum();

	}

	public abstract Stream<Info> infos();
}
