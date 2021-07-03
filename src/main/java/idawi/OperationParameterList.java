package idawi;

import java.util.ArrayList;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import toools.text.TextUtilities;

public class OperationParameterList extends ArrayList {
	public OperationParameterList(Object... parms) {
		for (Object o : parms) {
			add(o);
		}
	}

	public void reassignParameters(Class<?>[] types) {
		if (types.length != size())
			throw new IllegalArgumentException("expecting " + types.length + " parameters but got " + size());

		for (int i = 0; i < types.length; ++i) {
			var from = get(i);
			var to = types[i];

			if (!to.isAssignableFrom(from.getClass())) {
				set(i, convert(from, to));
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
			return fill(new LongOpenHashSet(), (String) from);
		} else if (LongList.class.isAssignableFrom(to) && from instanceof String) {
			return fill(new LongArrayList(), (String) from);
		} else {
			throw new IllegalArgumentException(from.getClass() + " cannot be converted to " + to);
		}
	}

	private static LongCollection fill(LongCollection c, String from) {
		for (var i : ((String) from).split(",")) {
			c.add(Long.parseLong(i));
		}

		return c;
	}

	public static OperationParameterList from(Operation operation, Object content, Class<?>[] types) {
		if (content == null && types.length == 0) {
			return new OperationParameterList();
		}
		
		if (content instanceof OperationParameterList) {
			var l = (OperationParameterList) content;
			l.reassignParameters(types);
			return l;
		} else {
			throw new IllegalArgumentException("when calling operation " + operation + ": an instance of "
					+ OperationParameterList.class + " was expected, but we got " + TextUtilities.toString(content)
					+ " of " + content.getClass());
		}
	}
}