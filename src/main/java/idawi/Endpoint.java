package idawi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Map;

import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import toools.util.Conversion;

public interface Endpoint<I, O> {
	void impl(MessageQueue in) throws Throwable;

	default void digitalTwin(MessageQueue in) throws Throwable {
		impl(in);
	}
	
	
	public default Class<I> inputSpecificationClass() {
		return inputSpecification(getClass());
	}

	public static <I, O, E extends Endpoint<I, O>> Class<I> inputSpecification(Class<E> c) {
		return ioSpecification(c, 0);
	}

	public static <E> Class<E> outputSpecification(Class<? extends Endpoint> c) {
		return ioSpecification(c, 1);
	}

	private static <E> Class<E> ioSpecification(Class<? extends Endpoint> c, int i) {
		var a = ((ParameterizedType) c.getGenericSuperclass()).getActualTypeArguments()[i];
		
		if (a instanceof ParameterizedType pa) {
			return (Class) pa.getRawType();
		}else {
			return (Class) a;
		}
	}

	public default I defaultParms() {
		try {
			return (I) inputSpecificationClass().getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	public default I parms(Message msg) {
		return Endpoint.from(msg.content, inputSpecificationClass());
	}

	public default I convert(Object o) {
		try {
			var parmClass = inputSpecificationClass();

			if (o instanceof Map<?, ?> map) {
				var r = defaultParms();

				for (var e : map.entrySet()) {
					var fieldName = (String) e.getKey();
					var targetField = parmClass.getField(fieldName);
					var destinationClass = targetField.getType();
					Object initialObject = e.getValue();

					if (!destinationClass.isAssignableFrom(initialObject.getClass())) {
						initialObject = Conversion.convert(initialObject, destinationClass);
					}

					targetField.set(r, initialObject);
				}

				return r;
			} else {
				return Conversion.convert(o, parmClass);
			}
		} catch (Exception err) {
			throw new RuntimeException(err);
		}
	}

	public static <T> T from(Object o, Class<T> t) {
		try {
			return t == o.getClass() ? (T) o : t.getConstructor(Object.class).newInstance(o);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException(e);
		}
	}
}
