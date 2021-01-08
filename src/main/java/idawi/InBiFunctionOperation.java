package idawi;

import java.lang.reflect.Field;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class InBiFunctionOperation extends InFieldOperation {
	private final BiFunction<Message, Consumer<Object>, Object> r;

	public InBiFunctionOperation(Field f, BiFunction<Message, Consumer<Object>, Object> m) {
		super(f);
		this.r = m;
	}

	@Override
	public void accept(Message msg, Consumer<Object> returns) {
		r.apply(msg, returns);
	}
}
