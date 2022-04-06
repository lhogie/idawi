package idawi.deploy;

import java.io.Serializable;

import idawi.ComponentDescriptor;

public class DeploymentPlan implements Serializable {
	 public final DiGraph<ComponentDescriptor> g = new DiGraph<>();

}
