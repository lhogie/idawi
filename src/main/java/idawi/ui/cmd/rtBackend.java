package idawi.ui.cmd;

import java.util.function.Consumer;

import idawi.Component;
import idawi.routing.RoutingService;

public class rtBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out)
			throws Throwable {
		n.lookupService(RoutingService.class).scheme.print(s -> out.accept(s));
	}
}
