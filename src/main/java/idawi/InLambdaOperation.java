package idawi;

import java.util.function.Consumer;

public class InLambdaOperation extends AbstractOperation {
	final OperationFunctionalInterface m;

	public InLambdaOperation(OperationFunctionalInterface m) {
		this.m = m;
	}

	@Override
	public OperationDescriptor signature() {
		return new OperationDescriptor(new Class[] { Message.class, Consumer.class });
	}

	@Override
	protected void acceptImpl(Message msg, Consumer<Object> returns) throws Throwable {
		m.accept(msg, returns);
	}
}
