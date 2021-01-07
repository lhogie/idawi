package idawi;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public class InRunnableOperation extends InFieldOperation {
	private final Runnable r;

	public InRunnableOperation(Field f, Runnable m) {
		super(f);
		this.r = m;
	}

	@Override
	public void accept(Message msg, Consumer<Object> returns) throws Throwable {
		r.run();
	}

	@Override
	public String getDescription() {
		return null;
	}
}
