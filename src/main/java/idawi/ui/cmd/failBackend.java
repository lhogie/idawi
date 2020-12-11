package idawi.ui.cmd;

import java.util.function.Consumer;

import idawi.Component;

public class failBackend extends CommandBackend {

	@Override
	public void runOnServer(Component n, Consumer<Object> out) throws Throwable {
		throw new Error("Some error");
	}
}
