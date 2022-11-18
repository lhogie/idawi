package idawi;

public abstract class Operation implements OperationFunctionalInterface {
	int nbCalls, nbFailures;
	double totalDuration;
	protected final OperationDescriptor descriptor = createOperationDescriptor();
	Service service;

	public Operation() {
		this.descriptor.implementationClass = getClass().getName();
		this.descriptor.name = getName();
		this.descriptor.description = getDescription();
	}

	protected OperationDescriptor createOperationDescriptor() {
		return new OperationDescriptor();
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

	public abstract String getDescription();

	public double avgDuration() {
		return totalDuration / nbCalls;
	}

	public int nbCalls() {
		return nbCalls;
	}

	public OperationDescriptor descriptor() {
		if (descriptor == null || descriptor.isOutOfDate()) {
			updateDescriptor();
		}

		return descriptor;
	}

	protected void updateDescriptor() {
		this.descriptor.nbCalls = this.nbCalls;
		this.descriptor.totalDuration = this.totalDuration;
	}

	public RemotelyRunningOperation exec(To to, boolean createQ, Object initialData) {
		Class operationClass = getClass();
		return service.exec(to.o(operationClass), createQ, initialData);
	}
	public RemotelyRunningOperation execfd(Object initialData) {
		return exec(new To(service.component), true, initialData);
	}


}
