package idawi.ui.cmd;

import java.util.function.Consumer;

import idawi.Component;

public class lnBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out) throws Throwable {
		n.outLinks().forEach(p -> out.accept(p));
	}
}
