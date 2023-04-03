package idawi.deploy;

import java.util.Set;

import idawi.Component;
import idawi.deploy.DeployerService.DeploymentRequest;

public class DeployEntry {
	public Component parent;
	public Set<DeploymentRequest> children;
}
