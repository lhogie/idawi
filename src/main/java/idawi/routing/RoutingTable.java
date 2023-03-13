package idawi.routing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import idawi.knowledge_base.ComponentRef;
import idawi.knowledge_base.KnowledgeBase;
import idawi.knowledge_base.NetworkMap;

public abstract class RoutingTable<E> implements Serializable {
	protected final Map<ComponentRef, E> map = new HashMap<>();

	public int getNbTargets() {
		return map.size();
	}

	public Set<ComponentRef> getTargets() {
		return map.keySet();
	}

	public abstract void discard(ComponentRef c);

	public abstract NetworkMap map();

	public E get(ComponentRef destination) {
		return map.get(destination);
	}



	public abstract void feedWith(Route r, ComponentRef me);

	public abstract void feedWith(ComponentRef d, KnowledgeBase registry);

}