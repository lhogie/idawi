package idawi.deploy;

import java.io.Serializable;

import idawi.knowledge_base.ComponentRef;

public class DeploymentPlan implements Serializable {
	 public final DiGraph<ComponentRef> g = new DiGraph<>();

}
