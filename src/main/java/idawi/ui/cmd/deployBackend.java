package idawi.ui.cmd;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.deploy.DeployerService.RemoteDeploymentRequest;
import idawi.knowledge_base.ComponentRef;

public class deployBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out) throws Throwable {

		List<ComponentRef> peers = Arrays.asList(cmdline.getOptionValue("--to").split(" *, *")).stream()
				.map(s -> new ComponentRef(s)).toList();
		peers.remove(n.ref());

		if (peers.isEmpty()) {
			out.accept("no peer");
			return;
		}

		boolean suicideWhenParentDie = !cmdline.isOptionSpecified("--autonomous");
		var reqs = RemoteDeploymentRequest.from(peers);
		reqs.forEach(r -> r.suicideWhenParentDie = suicideWhenParentDie);

		n.lookup(DeployerService.class).deploy(reqs, sdtout -> out.accept(sdtout),
				stderr -> out.accept("error: " + stderr), peerOk -> out.accept(peerOk + " is ready"));
	}
}
