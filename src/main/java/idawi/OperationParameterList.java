package idawi;

import java.util.ArrayList;

public class OperationParameterList extends ArrayList {
	public OperationParameterList(Object... parms) {
		for (Object o : parms) {
			add(o);
		}
	}

	public static OperationParameterList from(OperationStringParameterList l, Class<?>[] types)
			 {
		OperationParameterList r = new OperationParameterList();

		for (int i = 0; i < types.length; ++i) {
			r.add(fromString(l.get(i), types[i]));
		}

		return r;
	}

	private static Object fromString(String from, Class<?> to) throws IllegalArgumentException {
		if (to == String.class) {
			return from;
		} else if (to == double.class || to == Double.class) {
			return Double.valueOf(from);
		} else if (to == int.class || to == Integer.class) {
			return Long.valueOf(from);
		} else if (to == long.class || to == Long.class) {
			return Long.valueOf(from);
		} else if (to == int.class || to == Integer.class) {
			return Integer.valueOf(from);
		} else {
			throw new IllegalArgumentException("string cannot be converted to " + to.getClass());
		}
	}
}