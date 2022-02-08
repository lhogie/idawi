package idawi;

public abstract class Operation implements OperationFunctionalInterface {
	int nbCalls, nbFailures;
	double totalDuration;
	protected final OperationDescriptor descriptor;

	public Operation() {
		this.descriptor = new OperationDescriptor();
		this.descriptor.impl = getClass().getName();
	}

	public boolean isSystemOperation() {
		return getDeclaringServiceClass() != Service.class;
	}

	protected abstract Class<? extends Service> getDeclaringServiceClass();

	public abstract String getName();

	@Override
	public String toString() {
		return getDeclaringServiceClass().getName() + "/" + getName();
	}

	public abstract String getDescription() ;

	public double avgDuration() {
		return totalDuration / nbCalls;
	}

	public int nbCalls() {
		return nbCalls;
	}

	public OperationDescriptor descriptor() {
		this.descriptor.name = getName();
		this.descriptor.nbCalls = this.nbCalls;
		this.descriptor.totalDuration = this.totalDuration;
		return descriptor;
	}
}
