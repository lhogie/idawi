package idawi.routing;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import idawi.knowledge_base.ComponentRef;

public interface TargetComponents extends Predicate<ComponentRef>, Serializable {

	public static final TargetComponents all = c -> true;

	public static final class Unicast implements TargetComponents {
		public ComponentRef target;

		public Unicast(ComponentRef b) {
			if (b == null)
				throw new NullPointerException();

			this.target = b;
		}

		@Override
		public boolean test(ComponentRef c) {
			return c.equals(target);
		}

		@Override
		public String toString() {
			return target.toString();
		}

	};

	public static final class Multicast implements TargetComponents {
		Set<ComponentRef> target;

		public Multicast(Set<ComponentRef> targets) {
			if (targets == null)
				throw new NullPointerException();

			this.target = targets;
		}

		@Override
		public boolean test(ComponentRef c) {
			return target.contains(c);
		}

		@Override
		public String toString() {
			return target.toString();
		}
	}

	static TargetComponents fromString(String s) {
		var r = new Multicast(new HashSet<>());

		for (var a : s.split(" *, *")) {
			r.target.add(new ComponentRef(a));
		}

		return r;
	};
}
