package idawi.service;

import java.util.Set;

import idawi.ComponentInfo;
import idawi.Service;
import idawi.ServiceDescriptor;
import idawi.To;
import it.unimi.dsi.fastutil.ints.Int2LongMap;

public class ServiceStub {
	protected final Set<ComponentInfo> remoteComponents;
	protected final Service localService;
	private final Class<? extends Service> service;

	public ServiceStub(Service localService, Set<ComponentInfo> remoteComponents, Class<? extends Service> service) {
		this.localService = localService;
		this.remoteComponents = remoteComponents;
		this.service = service;
	}

	protected To to(String operation) {
		return new To(remoteComponents, service, operation);
	}

	public ServiceDescriptor descriptor() throws Throwable {
		return (ServiceDescriptor) localService.call(to("descriptor")).get();
	}

	public long nbMessagesReceived() throws Throwable {
		return (Long) localService.call(to("descriptor")).get();
	}

	public Int2LongMap second2nbMessages() throws Throwable {
		return (Int2LongMap) localService.call(to("second2nbMessages")).get();

	}

	public Set<String> listOperationNames() throws Throwable {
		return (Set<String>) localService.call(to("listOperationNames")).get();
	}
}