package idawi;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class InCallableOperation extends InFieldOperation {
	private final Callable r;

	public InCallableOperation(Field f, Callable m) {
		super(f);
		this.r = m;
	}

	@Override
	public void accept(Message msg, Consumer<Object> returns) throws Throwable {
		r.call();
	}

	@Override
	public String getDescription() {
		return null;
	}
}
