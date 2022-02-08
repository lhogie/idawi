package idawi;

import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2LongMap;

public class ServiceStub {
	protected final ServiceAddress to;
	protected final Service localService;

	public ServiceStub(Service localService, ServiceAddress to) {
		this.localService = localService;
		this.to = to;
	}

	/*
	 * protected To to(Class<? extends InInnerClassOperation> operation) { return
	 * new To(remoteComponents, operation); }
	 */
	private QueueAddress to(Class<? extends InnerOperation> operation) {
		return to.q(InnerOperation.name(operation));
	}

	protected OperationAddress toO(Class<? extends InnerOperation> operation) {
		return to.o(operation);
	}

	public ServiceDescriptor descriptor() throws Throwable {
		return (ServiceDescriptor) localService.exec(toO(Service.DescriptorOperation.class),
				localService.createQueue(), null).returnQ.get();
	}

	public long nbMessagesReceived() throws Throwable {
		return (Long) localService.exec(toO(Service.nbMessagesReceived.class), localService.createQueue(), null).returnQ.get();
	}

	public Int2LongMap second2nbMessages() throws Throwable {
		return (Int2LongMap) localService.exec(toO(Service.sec2nbMessages.class), localService.createQueue(), null).returnQ.get();

	}

	public Set<String> listOperationNames() throws Throwable {
		return (Set<String>) localService.exec(toO(Service.listOperationNames.class), localService.createQueue(), null).returnQ.get();
	}
}