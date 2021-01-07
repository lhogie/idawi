package idawi;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public class InMessageConsumerOperation extends InFieldOperation {
	private final Consumer<Message> r;

	public InMessageConsumerOperation(Field f, Consumer<Message> m) {
		super(f);
		this.r = m;
	}

	@Override
	public void accept(Message msg, Consumer<Object> returns) throws Throwable {
		r.accept(msg);
	}

	@Override
	public String getDescription() {
		return null;
	}
}
