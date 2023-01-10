package idawi;

import java.util.ArrayList;
import java.util.function.Function;

import toools.text.TextUtilities;
import toools.util.Conversion;
import toools.util.Conversion.Converter;

public class OperationParameterList extends ArrayList {
	public OperationParameterList(Object... parms) {
		for (Object o : parms) {
			add(o);
		}
	}

	public void convertTo(AbstractOperation operation, Class<?>[] types) {
//	public void convertTo(AbstractOperation operation, Class<?>[] types, Iterable<Converter<?, ?>> converters) {
		if (types.length != size())
			throw new IllegalArgumentException("operation " + operation + ": expecting types " + TextUtilities.toString(types) + " parameters but got " + TextUtilities.toString(this));

		for (int i = 0; i < types.length; ++i) {
			var initialObject = get(i);
			var destinationClass = types[i];

			if (!destinationClass.isAssignableFrom(initialObject.getClass())) {
//				set(i, Conversion.convert(initialObject, destinationClass, converters));
				set(i, Conversion.convert(initialObject, destinationClass));
			}
		}
	}

	public static OperationParameterList from(AbstractOperation operation, Object content, Class<?>[] types) {
		if (content == null && types.length == 0) {
			return new OperationParameterList();
		}

		if (content instanceof OperationParameterList) {
			var oarmList = (OperationParameterList) content;
			oarmList.convertTo(operation, types);
			return oarmList;
		} else {
			throw new IllegalArgumentException("when calling operation " + operation + ": an instance of "
					+ OperationParameterList.class + " was expected, but we got " + TextUtilities.toString(content)
					+ " of " + content.getClass());
		}
	}

}