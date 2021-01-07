package idawi;

import java.util.function.Consumer;

public abstract class Operation {
	int nbCalls;
	double totalDuration;
	protected final OperationDescriptor descriptor;

	public Operation() {
		this.descriptor = new OperationDescriptor();
		this.descriptor.impl = getClass().getName();
	}

	public abstract String getName();

	@Override
	public String toString() {
		return getName();
	}

	public abstract String getDescription();

	public double avgDuration() {
		return totalDuration / nbCalls;
	}

	public int nbCalls() {
		return nbCalls;
	}

	public OperationDescriptor descriptor() {
		this.descriptor.name  = getName();
		this.descriptor.nbCalls = this.nbCalls;
		this.descriptor.totalDuration = this.totalDuration;
		return descriptor;
	}

	public abstract void accept(Message msg, Consumer<Object> returns) throws Throwable;
}
