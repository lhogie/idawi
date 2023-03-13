package idawi;

import java.util.ArrayList;
import java.util.Arrays;

import toools.text.TextUtilities;
import toools.util.Conversion;

public class OperationParameterList extends ArrayList {
	public OperationParameterList(Object... parms) {
		for (Object o : parms) {
			add(o);
		}
	}

	public void convertTo(Class<?>[] types) {
//	public void convertTo(AbstractOperation operation, Class<?>[] types, Iterable<Converter<?, ?>> converters) {
		if (types.length != size())
			throw new IllegalArgumentException("expecting types " + TextUtilities.toString(types)
					+ " parameters but got " + TextUtilities.toString(this));

		for (int i = 0; i < types.length; ++i) {
			var initialObject = get(i);
			var destinationClass = types[i];

			if (!destinationClass.isAssignableFrom(initialObject.getClass())) {
//				set(i, Conversion.convert(initialObject, destinationClass, converters));
				set(i, Conversion.convert(initialObject, destinationClass));
			}
		}
	}

	public static OperationParameterList from(Object content, Class<?>[] types) {
		// method without parameters
		if (content == null && types.length == 0) {
			return new OperationParameterList();
		}

		if (content instanceof OperationParameterList) {
			var parmList = (OperationParameterList) content;
			parmList.convertTo(types);
			return parmList;
		} else if (types.length == 1) {
			var parmList = new OperationParameterList();
			parmList.add(content);
			parmList.convertTo(types);
			return parmList;
		} else {
			throw new IllegalArgumentException("can't match " + content + " and " + Arrays.toString(types));
		}
	}

}