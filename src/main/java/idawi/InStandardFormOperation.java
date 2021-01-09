package idawi;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public class InStandardFormOperation extends InFieldOperation {
	private final OperationStandardForm r;

	public InStandardFormOperation(Field f, OperationStandardForm m) {
		super(f);
		this.r = m;
	}

	@Override
	public void accept(Message msg, Consumer<Object> returns) throws Throwable {
		r.accept(msg, returns);
	}
}
