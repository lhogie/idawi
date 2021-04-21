package idawi.routing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import idawi.ComponentDescriptor;
import idawi.RegistryService;
import idawi.Route;
import idawi.map.NetworkMap;

public abstract class RoutingTable<E> implements Serializable {
	protected final Map<ComponentDescriptor, E> map = new HashMap<>();

	public int getNbTargets() {
		return map.size();
	}

	public Set<ComponentDescriptor> getTargets() {
		return map.keySet();
	}

	public abstract void discard(ComponentDescriptor c);

	public abstract NetworkMap map();

	public E get(ComponentDescriptor destination) {
		return map.get(destination);
	}



	public abstract void feedWith(Route r, ComponentDescriptor me);

	public abstract void feedWith(ComponentDescriptor d, RegistryService registry);

}