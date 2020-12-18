package idawi.ui.cmd;

import java.util.function.Consumer;

import idawi.Component;
import idawi.service.registry.RegistryService;

public class dotBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out)
			throws Throwable {
		out.accept(n.lookupService(RegistryService.class).localMap.toDot());
	}
}
