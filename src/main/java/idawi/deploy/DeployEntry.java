package idawi.deploy;

import java.util.List;
import java.util.Set;

import idawi.deploy.DeployerService.DeploymentRequest;
import idawi.knowledge_base.ComponentRef;

public class DeployEntry {
	public ComponentRef parent;
	public Set<DeploymentRequest> chilren;
}
