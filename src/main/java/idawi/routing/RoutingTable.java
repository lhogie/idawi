package idawi.routing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.knowledge_base.KnowledgeBase;
import idawi.knowledge_base.NetworkDigitalTwin;

public abstract class RoutingTable<E> implements Serializable {
	protected final Map<Component, E> map = new HashMap<>();

	public int getNbTargets() {
		return map.size();
	}

	public Set<Component> getTargets() {
		return map.keySet();
	}

	public abstract void discard(Component c);

	public abstract NetworkDigitalTwin map();

	public E get(Component destination) {
		return map.get(destination);
	}

	public abstract void feedWith(Route r, Component me);

	public abstract void feedWith(Component d, KnowledgeBase registry);

}