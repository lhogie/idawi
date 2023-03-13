package idawi.ui.cmd;

import java.util.function.Consumer;

import idawi.Component;
import idawi.knowledge_base.MapService;

public class lnBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out) throws Throwable {
		n.lookup(MapService.class).map.outNeighbors(n.ref()).forEach(p -> out.accept(p));
	}
}
