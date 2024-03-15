package idawi;

import idawi.service.local_view.EndpointDescriptor;
import toools.SizeOf;

public abstract class AbstractEndpoint implements Endpoint, SizeOf {
	int nbCalls, nbFailures;
	double totalDuration;
	private EndpointDescriptor descriptor;
	Service service;

	protected EndpointDescriptor createDescriptor() {
		return new EndpointDescriptor();
	}

	public boolean isSystemOperation() {
		return getDeclaringServiceClass() != Service.class;
	}

	protected abstract Class<? extends Service> getDeclaringServiceClass();

	public abstract String getName();

	@Override
	public String toString() {
		return service + "/" + getName();
	}

	public abstract String getDescription();

	public double avgDuration() {
		return totalDuration / nbCalls;
	}

	public int nbCalls() {
		return nbCalls;
	}

	public EndpointDescriptor descriptor() {
		if (descriptor == null) {
			this.descriptor = createDescriptor();
			this.descriptor.implementationClass = getClass().getName();
			this.descriptor.name = getName();
			this.descriptor.description = getDescription();
		} else {
			updateDescriptor();
		}

		return descriptor;
	}

	protected void updateDescriptor() {
		this.descriptor.nbCalls = this.nbCalls;
		this.descriptor.totalDuration = this.totalDuration;
	}

	@Override
	public long sizeOf() {
		return 48;
	}

}
