package idawi;

import java.util.function.Consumer;
import java.util.function.Predicate;

import idawi.messaging.Message;
import idawi.service.local_view.EndpointDescriptor;
import toools.SizeOf;

public abstract class AbstractEndpoint<I, O> implements Endpoint<I, O>, SizeOf {
	int nbCalls, nbFailures;
	double totalDuration;
	private EndpointDescriptor descriptor;
	Service service;

	public String getFullyQualifiedName() {
		return service.component + "/" + service.getFriendlyName() + "/" + getName();
	}

	public boolean isGranted(Component requester) {
		return authorizations.test(requester);
	}

	public final Predicate<Component> authorizations = c -> true;

	protected EndpointDescriptor createDescriptor() {
		return new EndpointDescriptor();
	}

	public boolean isSystemOperation() {
		return getDeclaringServiceClass() != Service.class;
	}

	protected void reply(Object o, Message<?> m) {
		service.send(o, m.replyTo);
	}

	protected void reply(Message<?> m, Consumer<Message<?>> c) {
		service.send(msg -> {
			msg.qAddr = m.replyTo;
			c.accept(m);
		});
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
