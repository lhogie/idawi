package idawi;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public class InFIOperation extends InFieldOperation {
	private final OperationFI r;

	public InFIOperation(Field f, OperationFI m) {
		super(f);
		this.r = m;
	}

	@Override
	public void accept(Message msg, Consumer<Object> returns) throws Throwable {
		r.accept(msg, returns);
	}
}
