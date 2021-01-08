package idawi;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.function.Consumer;

public class SerializableFieldGetter extends InFieldOperation {
	private final Serializable r;

	public SerializableFieldGetter(Field f, Serializable m) {
		super(f);
		this.r = m;
	}

	@Override
	public void accept(Message msg, Consumer<Object> returns) throws Throwable {
		returns.accept(r);
	}
}
