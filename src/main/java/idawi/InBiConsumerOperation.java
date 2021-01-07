package idawi;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class InBiConsumerOperation extends InFieldOperation {
	private final BiConsumer<Message, Consumer<Object>> r;

	public InBiConsumerOperation(Field f, BiConsumer<Message, Consumer<Object>> m) {
		super(f);
		this.r = m;
	}

	@Override
	public void accept(Message msg, Consumer<Object> returns) throws Throwable {
		r.accept(msg, returns);
	}

	@Override
	public String getDescription() {
		return null;
	}
}
