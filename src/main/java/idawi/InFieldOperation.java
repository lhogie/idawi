package idawi;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class InFieldOperation extends Operation {
	private final Field field;

	public InFieldOperation(Field m) {
		super(m.getDeclaringClass());
		m.setAccessible(true);
		this.field = m;
	}

	@Override
	public String toString() {
		return field.getDeclaringClass().getName() + "." + getName();
	}

	@Override
	public String getName() {
		return field.getName();
	}

	@Override
	public String getDescription() {
		return getClass().getName() + " " + getName();
	}

	static InFieldOperation toOperation(Field f, Object v) {
		if (v == null) {
			return new SerializableFieldGetter(f, (Serializable) v);
		} else if (is(v, Runnable.class)) {
			return new InRunnableOperation(f, (Runnable) v);
		} else if (is(v, Callable.class)) {
			return new InCallableOperation(f, (Callable) v);
		} else if (is(v, Consumer.class, Message.class)) {
			return new InMessageConsumerOperation(f, (Consumer<Message>) v);
		} else if (is(v, BiConsumer.class, Message.class, Consumer.class)) {
			return new InBiConsumerOperation(f, (BiConsumer<Message, Consumer<Object>>) v);
		} else if (is(v, Supplier.class)) {
			return new InSupplierOperation(f, (Supplier) v);
		} else if (is(v, BiFunction.class, Message.class, Consumer.class, Object.class)) {
			return new InBiFunctionOperation(f, (BiFunction<Message, Consumer<Object>, Object>) v);
		} else if (is(v, OperationStandardForm.class)) {
			return new InStandardFormOperation(f, (OperationStandardForm) v);
		} else if (v instanceof Serializable) {
			// an annotated field can be requested as an operation
			return new SerializableFieldGetter(f, (Serializable) v);
		} else {
			return null;
		}
	}

	private static boolean is(Object v, Class c, Class... genericType) {
		return c.isAssignableFrom(v.getClass());
	}

	private static boolean is2(Object v, Class c, Class... genericType) {
		if (!c.isAssignableFrom(v.getClass())) {
			return false;
		}

		Type[] gi = v.getClass().getGenericInterfaces();

		if (gi.length != genericType.length) {
			return false;
		}

		for (int i = 0; i < gi.length; ++i) {
			if (gi[i] != genericType[i]) {
				return false;
			}
		}

		return true;
	}
}
