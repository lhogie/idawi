package idawi;

public abstract class Operation implements OperationStandardForm {
	int nbCalls;
	double totalDuration;
	protected final OperationDescriptor descriptor;
	protected final Class<? extends Service> declaringClass;

	public Operation(Class<? extends Service> declaringClass) {
		this.declaringClass = declaringClass;
		this.descriptor = new OperationDescriptor();
		this.descriptor.impl = getClass().getName();
	}

	public boolean isSystemOperation() {
		return declaringClass != Service.class;
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
		this.descriptor.name = getName();
		this.descriptor.nbCalls = this.nbCalls;
		this.descriptor.totalDuration = this.totalDuration;
		return descriptor;
	}
}
