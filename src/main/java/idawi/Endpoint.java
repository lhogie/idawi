package idawi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.Map;

import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import toools.reflect.Clazz;
import toools.util.Conversion;

public interface Endpoint<I, O> {

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface EDescription {
		String value();
	}

	public default String getDescription() {
		return getClass().getAnnotation(EDescription.class).value();
	}

	void impl(MessageQueue in) throws Throwable;

	default void digitalTwin(MessageQueue in) throws Throwable {
		impl(in);
	}

	public default Class<I> inputSpecification() {
		return inputSpecification(getClass());
	}

	public default Class<O> outputSpecification() {
		return outputSpecification(getClass());
	}

	public static <I, O, E extends Endpoint<I, O>> Class<I> inputSpecification(Class<E> c) {
		return (Class<I>) ioSpecification(c, 0);
	}

	public static <I, O, E extends Endpoint<I, O>> Class<O> outputSpecification(Class<E> c) {
		return (Class<O>) ioSpecification(c, 1);
	}

	private static <I, O, E extends Endpoint<I, O>> Class<?> ioSpecification(Class<E> endpointClass, int i) {
		var sc = endpointClass.getGenericSuperclass();

		if (sc instanceof ParameterizedType pa) {
			var ioType = pa.getActualTypeArguments()[i];

			if (ioType instanceof ParameterizedType parameterizedIOType) {
				return (Class) parameterizedIOType.getRawType();
			} else {
				return (Class) ioType;
			}
		} else {
			return null;
		}
	}

	public default I getInputFrom(Message<?> msg) {
		return (I) msg.content;
	}

	public static <I, O, E extends Endpoint<I, O>> I from(Object input, Class<E> e) {
		Class<I> inputSpec = Endpoint.inputSpecification(e);

		if (inputSpec == null || inputSpec.isAssignableFrom(input.getClass())) {
			return (I) input;
		} else if (input instanceof Map<?, ?> map) {
			var inputInstance = Clazz.makeInstance(inputSpec);

			for (var k : new HashSet<>(map.keySet())) {
				var v = map.get(k);

				if (k.equals("p")) {
					inputInstance = Conversion.convert(v, inputSpec);
					map.remove(k);
					break;
				} else if (k.toString().startsWith("p.")) {
					var propName = k.toString().substring(2);

					if (!propName.isEmpty()) {
						try {
							var field = inputSpec.getDeclaredField(propName);
							var fieldValue = Conversion.convert(v, field.getType());
							field.set(inputInstance, fieldValue);
							map.remove(k);
						} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
								| IllegalAccessException err) {
							throw new RuntimeException(err);
						}
					}
				}
			}

			return inputInstance;
		} else {
			return Conversion.convert(input, inputSpec);
		}
	}

}
