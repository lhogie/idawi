package idawi;

import java.util.ArrayList;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import toools.text.TextUtilities;

public class OperationParameterList extends ArrayList {
	public OperationParameterList(Object... parms) {
		for (Object o : parms) {
			add(o);
		}
	}

	private static void reassignParameters(OperationParameterList l, Class<?>[] types) {
		for (int i = 0; i < types.length; ++i) {
			var from = l.get(i);
			var to = types[i];

			if (!to.isAssignableFrom(from.getClass())) {
				l.set(i, convert(from, to));
			}
		}
	}

	private static Object convert(Object from, Class<?> to) throws IllegalArgumentException {
		if (to == double.class || to == Double.class) {
			return Double.valueOf(from.toString());
		} else if (to == int.class || to == Integer.class) {
			return Long.valueOf(from.toString());
		} else if (to == long.class || to == Long.class) {
			return Long.valueOf(from.toString());
		} else if (to == int.class || to == Integer.class) {
			return Integer.valueOf(from.toString());
		} else if (LongSet.class.isAssignableFrom(to) && from instanceof String) {
			var l = new LongOpenHashSet();

			for (var i : ((String) from).split(" +")) {
				l.add(Long.parseLong(i));
			}

			return l;
		} else {
			throw new IllegalArgumentException(from.getClass() + " cannot be converted to " + to);
		}
	}

	public static OperationParameterList from(Operation operation, Object content, Class<?>[] types) {
		if (content == null) {
			return new OperationParameterList();
		} else if (content instanceof OperationParameterList) {
			var l = (OperationParameterList) content;
			reassignParameters(l, types);
			return l;
		} else {
			throw new IllegalArgumentException("when calling operation " + operation + ": an instance of "
					+ OperationParameterList.class + " was expected, but we got " + TextUtilities.toString(content)
					+ " of " + content.getClass());
		}
	}
}