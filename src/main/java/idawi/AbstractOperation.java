package idawi;

import idawi.knowledge_base.OperationDescriptor;
import toools.SizeOf;

public abstract class AbstractOperation implements Operation, SizeOf {
	int nbCalls, nbFailures;
	double totalDuration;
	protected final OperationDescriptor descriptor = createDescriptor();
	Service service;

	public AbstractOperation() {
		this.descriptor.implementationClass = getClass().getName();
		this.descriptor.name = getName();
		this.descriptor.description = getDescription();
	}


	protected OperationDescriptor createDescriptor() {
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
		updateDescriptor();
		return descriptor;
	}

	protected void updateDescriptor() {
		this.descriptor.nbCalls = this.nbCalls;
		this.descriptor.totalDuration = this.totalDuration;
	}

	@Override
	public long sizeOf() {
		return 0;
	}

}
