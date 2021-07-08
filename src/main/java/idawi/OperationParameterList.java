package idawi;

import java.util.ArrayList;

import it.unimi.dsi.fastutil.longs.LongCollection;
import toools.text.TextUtilities;
import toools.util.Conversion;

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
				set(i, Conversion.convert(from, to));
			}
		}
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