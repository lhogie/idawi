package idawi;

import java.util.Set;

import idawi.AsMethodOperation.OperationID;
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
	private QueueAddress to(Class<? extends InnerClassOperation> operation) {
		return to.q(operation.operationName);
	}

	protected OperationAddress toO(Class<? extends InnerClassOperation> operation) {
		return to.o(operation);
	}

	public ServiceDescriptor descriptor() throws Throwable {
		return (ServiceDescriptor) localService.start(toO(Service.DescriptorOperation.class), true, null).returnQ.get();
	}

	public long nbMessagesReceived() throws Throwable {
		return (Long) localService.start(toO(Service.nbMessagesReceived.class), true, null).returnQ.get();
	}

	public Int2LongMap second2nbMessages() throws Throwable {
		return (Int2LongMap) localService.start(toO(Service.sec2nbMessages.class), true, null).returnQ.get();

	}

	public Set<String> listOperationNames() throws Throwable {
		return (Set<String>) localService.start(toO(Service.listOperationNames.class), true, null).returnQ.get();
	}
}