package idawi.ui.cmd;

import java.util.function.Consumer;

import idawi.Component;

public class killBackend extends CommandBackend {

	@Override
	public void runOnServer(Component thing, Consumer<Object> out)
			throws Throwable {
		System.exit(0);
	}
}
