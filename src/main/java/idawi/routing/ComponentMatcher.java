package idawi.routing;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import idawi.Component;
import idawi.service.SystemService;
import idawi.service.local_view.LocalViewService;

/**
 * The matcher runs on the real component to check if it should try to execute
 * the requested operation.
 *
 */
public abstract class ComponentMatcher implements Predicate<Component>, Serializable, URLable {

	public static final ComponentMatcher all = new all();

	@Override
	public String toString() {
		return toURLElement();
	}

	public static class all extends ComponentMatcher {

		@Override
		public boolean test(Component t) {
			return true;
		}

		@Override
		public String toURLElement() {
			return "";
		}

	};

	public static ComponentMatcher multicast(Set<Component> target) {
		return new multicast(target);
	}

	public static class multicast extends ComponentMatcher {
		Set<Component> target;

		multicast(Set<Component> target) {
			this.target = target;
		}

		@Override
		public boolean test(Component t) {
			return target.contains(t);
		}

		@Override
		public String toURLElement() {
			return target.toString();
		}
	}

	public static ComponentMatcher regex(String regex) {
		return new regex(regex);
	}

	public static class regex extends ComponentMatcher {
		String regex;

		regex(String regex) {
			this.regex = regex;
		}

		@Override
		public boolean test(Component t) {
			return regex.matches(t.name());
		}

		@Override
		public String toURLElement() {
			return "regex:" + regex;
		}
	}

	public static ComponentMatcher atLeastNCores(int nbCores) {
		return new minCores(nbCores);
	};

	public static class minCores extends ComponentMatcher {
		int n;

		minCores(int n) {
			this.n = n;
		}

		@Override
		public boolean test(Component c) {
			return c.need(SystemService.class).nbCores >= n;
		}

		@Override
		public String toURLElement() {
			return "minCores:" + n;
		}
	}

	public static ComponentMatcher fromString(String s, LocalViewService lookup) {
		s = s.trim();

		if (s.isEmpty()) {
			return ComponentMatcher.all;
		} else {
			int pos = s.indexOf('.');

			if (pos == -1) {
				return multicast(new HashSet<Component>(Arrays.stream(s.split(" *, *")).map(ref -> {
					var c = lookup.lookup(ref);

					if (c == null) {
						lookup.localTwin(c = new Component(ref, lookup));
					}

					return c;
				}).toList()));
			} else {
				var prefix = s.substring(0, pos);
				var value = s.substring(pos + 1);

				if (prefix.equals("regex")) {
					return regex(value);
				} else if (prefix.equals("minCores")) {
					return atLeastNCores(Integer.valueOf(value));
				} else {
					throw new IllegalArgumentException("unknown matcher prefix: " + prefix);
				}
			}
		}
	}

	public static ComponentMatcher unicast(Component to) {
		return multicast(Set.of(to));
	}

}
