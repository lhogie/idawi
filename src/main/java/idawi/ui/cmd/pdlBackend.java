package idawi.ui.cmd;

import java.util.function.Consumer;

import idawi.Component;

public class pdlBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out)
			throws Throwable {
		out.accept(n.descriptor().toCDL());
	}
}
