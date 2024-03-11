package idawi.ui.cmd;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import idawi.Component;
import idawi.deploy.DeployerService;
import toools.net.SSHParms;

public class deployBackend extends CommandBackend {

	@Override
	public void runOnServer(Component localComponent, Consumer<Object> out) throws Throwable {

		List<SSHParms> peers = Arrays.asList(cmdline.getOptionValue("--to").split(" *, *")).stream()
				.map(s -> SSHParms.fromSSHString(s)).toList();

//		peers.removeLinksHeadingTo(n);

		if (peers.isEmpty()) {
			out.accept("no peer");
			return;
		}

		boolean autonomous = cmdline.isOptionSpecified("--autonomous");

		localComponent.service(DeployerService.class).deployViaSSH(peers, sdtout -> out.accept(sdtout),
				stderr -> out.accept("error: " + stderr), peerOk -> out.accept(peerOk + " is ready"),
				err -> err.printStackTrace());
	}
}
