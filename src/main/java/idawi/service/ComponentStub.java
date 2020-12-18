package idawi.service;

import java.util.Set;

import idawi.ComponentInfo;
import idawi.Service;

public class ComponentStub {
	protected final Set<ComponentInfo> remoteComponent;
	protected final Service localService;

	public ComponentStub(Service localService, ComponentInfo remoteComponent) {
		this.localService = localService;
		this.remoteComponent = Set.of(remoteComponent);
	}
}