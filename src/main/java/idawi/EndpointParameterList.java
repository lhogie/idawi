package idawi;

import java.util.ArrayList;
import java.util.Arrays;

import toools.text.TextUtilities;
import toools.util.Conversion;

public class EndpointParameterList extends ArrayList {
	public EndpointParameterList(Object... parms) {
		for (Object o : parms) {
			add(o);
		}
	}

	public void convertTo(Class<?>[] types, Class<? extends TypedInnerClassEndpoint> class1) {
//	public void convertTo(AbstractOperation operation, Class<?>[] types, Iterable<Converter<?, ?>> converters) {
		if (types.length != size())
			throw new IllegalArgumentException("expecting types " + TextUtilities.toString(types)
					+ " parameters but got " + TextUtilities.toString(this));

		for (int i = 0; i < types.length; ++i) {
			var initialObject = get(i);
			var destinationClass = types[i];

			if (!destinationClass.isAssignableFrom(initialObject.getClass())) {
//				set(i, Conversion.convert(initialObject, destinationClass, converters));
				try {
					set(i, Conversion.convert(initialObject, destinationClass));
				} catch (Exception err) {
					throw new RuntimeException(class1 + ": cannot convert from " + initialObject.getClass() + " to " + destinationClass);
				}
			}
		}
	}

	public static EndpointParameterList from(Object content, Class<?>[] types, Class<? extends TypedInnerClassEndpoint> class1) {
		// method without parameters
		if (content == null && types.length == 0) {
			return new EndpointParameterList();
		}

		if (content instanceof EndpointParameterList) {
			var parmList = (EndpointParameterList) content;
			parmList.convertTo(types, class1);
			return parmList;
		} else if (types.length == 1) {
			var parmList = new EndpointParameterList();
			parmList.add(content);
			parmList.convertTo(types, class1);
			return parmList;
		} else {
			throw new IllegalArgumentException("can't match " + content + " and " + Arrays.toString(types));
		}
	}

}