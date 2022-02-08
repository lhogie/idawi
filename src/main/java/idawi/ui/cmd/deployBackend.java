package idawi.ui.cmd;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.service.DeployerService;

public class deployBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out)
			throws Throwable {

		List<ComponentDescriptor> peers = ComponentDescriptor
				.fromPDL(Arrays.asList(cmdline.getOptionValue("--to").split(" *, *")));
		peers.remove(n.descriptor());

		if (peers.isEmpty()) {
			out.accept("no peer");
			return;
		}

		boolean suicideWhenParentDie = ! cmdline.isOptionSpecified("--autonomous");
		boolean printRsync = ! cmdline.isOptionSpecified("--rsync");
		n.lookup(DeployerService.class).deploy(peers, suicideWhenParentDie, 2000,
				printRsync, out, peerOk -> out.accept(peerOk + " is ready"));
	}
}
