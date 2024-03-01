package idawi.ui.cmd;

import java.util.Arrays;
import java.util.function.Consumer;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.deploy.DeployerService.RemoteDeploymentRequest;
import idawi.service.DigitalTwinService;

public class deployBackend extends CommandBackend {

	@Override
	public void runOnServer(Component localComponent, Consumer<Object> out) throws Throwable {

		var peers = Arrays.asList(cmdline.getOptionValue("--to").split(" *, *")).stream().map(s -> {
			var c = new Component();
			c.friendlyName = s;
			var dt = new DigitalTwinService(c);
			dt.host = localComponent.localView();
			c.friendlyName = s;
			return c;
		})
				.toList();
//		peers.removeLinksHeadingTo(n);

		if (peers.isEmpty()) {
			out.accept("no peer");
			return;
		}

		var reqs = RemoteDeploymentRequest.from(peers);
		reqs.forEach(r -> r.target.dt().info().suicideWhenParentDie = !cmdline.isOptionSpecified("--autonomous"));

		localComponent.service(DeployerService.class).deployRemotely(reqs, sdtout -> out.accept(sdtout),
				stderr -> out.accept("error: " + stderr), peerOk -> out.accept(peerOk + " is ready"));
	}
}
