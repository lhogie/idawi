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
	protected QueueAddress to(OperationID operation) {
		return new QueueAddress(to.getNotYetReachedExplicitRecipients(), operation.declaringService,
				operation.operationName);
	}

	public ServiceDescriptor descriptor() throws Throwable {
		return (ServiceDescriptor) localService.trigger(to, Service.descriptor, true, null).returnQ.get();
	}

	public long nbMessagesReceived() throws Throwable {
		return (Long) localService.trigger(to, Service.nbMessagesReceived, true, null).returnQ.get();
	}

	public Int2LongMap second2nbMessages() throws Throwable {
		return (Int2LongMap) localService.trigger(to, Service.sec2nbMessages, true, null).returnQ.get();

	}

	public Set<String> listOperationNames() throws Throwable {
		return (Set<String>) localService.trigger(to, Service.listOperationNames, true, null).returnQ.get();
	}
}