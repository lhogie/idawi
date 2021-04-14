package idawi;

import java.util.ArrayList;

import toools.text.TextUtilities;

public class OperationParameterList extends ArrayList {
	public OperationParameterList(Object... parms) {
		for (Object o : parms) {
			add(o);
		}
	}

	public static OperationParameterList from(OperationStringParameterList l, Class<?>[] types) {
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

	public static OperationParameterList toParmsList(Operation operation, Object content, Class<?>[] types) {
		if (content instanceof OperationParameterList) {
			return (OperationParameterList) content;
		} else if (content instanceof OperationStringParameterList) {
			return from((OperationStringParameterList) content, types);
		} else {
			throw new IllegalArgumentException("when calling operation " + operation + ": an instance of " + OperationParameterList.class + " was expected, but we got " + TextUtilities.toString(content)
					+ (content == null ? "" : " of " + content.getClass()));
		}
	}
}