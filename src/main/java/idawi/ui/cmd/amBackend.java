package idawi.ui.cmd;

import java.util.function.Consumer;

import idawi.Component;
import idawi.net.NetworkingService;

public class amBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out)
			throws Throwable {
		n.lookupService(NetworkingService.class).aliveMessages.values().forEach(msg -> out.accept(msg.toString()));
	}
}
