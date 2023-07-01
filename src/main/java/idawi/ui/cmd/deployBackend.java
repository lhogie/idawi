package idawi.ui.cmd;

import java.util.Arrays;
import java.util.function.Consumer;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.deploy.DeployerService.RemoteDeploymentRequest;

public class deployBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out) throws Throwable {

		var peers = Arrays.asList(cmdline.getOptionValue("--to").split(" *, *")).stream().map(s -> new Component(s, n))
				.toList();
//		peers.removeLinksHeadingTo(n);

		if (peers.isEmpty()) {
			out.accept("no peer");
			return;
		}

		boolean suicideWhenParentDie = !cmdline.isOptionSpecified("--autonomous");
		var reqs = RemoteDeploymentRequest.from(peers);
		reqs.forEach(r -> r.target.dt().info().suicideWhenParentDie = suicideWhenParentDie);

		n.need(DeployerService.class).deployRemotely(reqs, sdtout -> out.accept(sdtout),
				stderr -> out.accept("error: " + stderr), peerOk -> out.accept(peerOk + " is ready"));
	}
}
