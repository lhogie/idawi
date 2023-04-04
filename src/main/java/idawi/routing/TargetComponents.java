package idawi.routing;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import idawi.Component;
import idawi.knowledge_base.DigitalTwinService;
import toools.text.TextUtilities;

public interface TargetComponents extends Predicate<Component>, Serializable {

	public static final TargetComponents all = new TargetComponents() {

		@Override
		public boolean test(Component t) {
			return true;
		}

		@Override
		public String toString() {
			return "*";
		}

	};

	public static final class Unicast implements TargetComponents {
		public Component target;

		public Unicast(Component b) {
			if (b == null)
				throw new NullPointerException();

			this.target = b;
		}

		@Override
		public boolean test(Component c) {
			return c.equals(target);
		}

		@Override
		public String toString() {
			return target.toString();
		}

	};

	public static final class Multicast implements TargetComponents {
		Set<Component> target;

		public Multicast(Set<Component> targets) {
			if (targets == null)
				throw new NullPointerException();

			this.target = targets;
		}

		@Override
		public boolean test(Component c) {
			return target.contains(c);
		}

		@Override
		public String toString() {
			return TextUtilities.concat(",", List.of(target));
		}
	}

	static TargetComponents fromString(String s, DigitalTwinService lookup) {
		s = s.trim();

		if (s.isEmpty()) {
			return TargetComponents.all;
		} else {
			var r = new Multicast(new HashSet<>());

			for (var componentName : s.split(" *, *")) {
				var c = lookup.lookup(componentName);

				if (c == null) {
					lookup.add(c = new Component(componentName));
				}

				r.target.add(c);
			}

			return r;
		}
	}

	static TargetComponents unicast(Component component) {
		return new Unicast(component);
	};
}
