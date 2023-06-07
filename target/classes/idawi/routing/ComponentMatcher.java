package idawi.routing;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import idawi.Component;
import idawi.knowledge_base.DigitalTwinService;
import idawi.service.SystemService;

/**
 * The matcher runs on the real component to check if it should try to execute the requested operation.
 *
 */
public interface ComponentMatcher extends Predicate<Component>, Serializable {

	public static final ComponentMatcher all = c -> true;

	public static ComponentMatcher one(Component target) {
		return c -> target.equals(c);
	};

	public static ComponentMatcher atLeastNCores(int nbCores) {
		return c -> c.lookup(SystemService.class).nbCores >= nbCores;
	};

	public static ComponentMatcher among(Set<Component> target) {
		return c -> target.contains(c);
	};

	static ComponentMatcher fromString(String s, DigitalTwinService lookup) {
		s = s.trim();

		if (s.isEmpty()) {
			return ComponentMatcher.all;
		} else {
			var r = new HashSet<Component>();

			for (var componentName : s.split(" *, *")) {
				var c = lookup.lookup(componentName);

				if (c == null) {
					lookup.add(c = new Component(componentName));
				}

				r.add(c);
			}

			return among(r);
		}
	}

}
