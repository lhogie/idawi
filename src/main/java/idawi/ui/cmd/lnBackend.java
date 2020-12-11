package idawi.ui.cmd;

import java.util.function.Consumer;

import idawi.Component;
import idawi.net.NetworkingService;

public class lnBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out) throws Throwable {

		if (cmdline.isOptionSpecified("--protocols")) {
			n.lookupService(NetworkingService.class).transport.peers2()
					.forEach((p, protocols) -> out.accept(p + "\t" + protocols));
		} else {
			n.lookupService(NetworkingService.class).transport.neighbors().forEach(p -> out.accept(p));
		}
	}
}
