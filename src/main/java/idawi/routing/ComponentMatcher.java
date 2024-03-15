package idawi.routing;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import idawi.Component;
import idawi.service.SystemService;
import idawi.service.local_view.LocalViewService;
import toools.SizeOf;

/**
 * The matcher runs on the real component to check if it should try to execute
 * the requested operation.
 *
 */
public abstract class ComponentMatcher implements Predicate<Component>, Serializable, URLable, SizeOf {

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

		@Override
		public long sizeOf() {
			return 1;
		}

	};

	public static ComponentMatcher multicast(Collection<Component> target) {
		return new multicast(target);
	}

	public static class multicast extends ComponentMatcher {
		public Collection<Component> target;

		public multicast(Collection<Component> target) {
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

		@Override
		public long sizeOf() {
			return target.size() * 8;
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
			return regex.matches(t.friendlyName);
		}

		@Override
		public String toURLElement() {
			return "regex:" + regex;
		}

		@Override
		public long sizeOf() {
			return regex.length() + 4;
		}

	}

	public static ComponentMatcher atLeastNCores(int nbCores) {
		return new minCores(nbCores);
	};

	public static class minCores extends ComponentMatcher {
		int nbCores;

		public minCores(int n) {
			this.nbCores = n;
		}

		@Override
		public boolean test(Component c) {
			return c.service(SystemService.class).nbCores >= nbCores;
		}

		@Override
		public String toURLElement() {
			return "minCores:" + nbCores;
		}

		@Override
		public long sizeOf() {
			return 4;
		}

	}

	public static ComponentMatcher fromString(String s, LocalViewService lookup) {
		s = s.trim();

		if (s.isEmpty()) {
			return ComponentMatcher.all;
		} else {
			int pos = s.indexOf('.');

			if (pos == -1) {
				return multicast(new HashSet<>(Arrays.stream(s.split(" *, *")).map(name -> {
					var c = lookup.g.findComponentByFriendlyName(name);

					if (c == null) {
						c = new Component();
						c.friendlyName = name;
						lookup.g.ensureExists(c);
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
