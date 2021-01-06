package idawi;

import java.io.Serializable;

public class DeploymentPlan implements Serializable {
	private final Graph<ComponentDescriptor> g = new Graph<>();

	public void addChild(ComponentDescriptor parent, ComponentDescriptor child) {
		g.add(parent, child);
	}

	public void removeChild(ComponentDescriptor parent, ComponentDescriptor child) {
		g.remove(parent, child);
	}
	

}
