package idawi;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class InSupplierOperation extends InFieldOperation {
	private final Supplier r;

	public InSupplierOperation(Field f, Supplier m) {
		super(f);
		this.r = m;
	}

	@Override
	public void accept(Message msg, Consumer<Object> returns) throws Throwable {
		returns.accept(r.get());
	}

	@Override
	public String getDescription() {
		return null;
	}
}
