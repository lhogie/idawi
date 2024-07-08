package idawi.ui.cmd;

import java.util.function.Consumer;

import idawi.Component;

public class dummyBackend extends CommandBackend {

	@Override
	public void runOnServer(Component thing, Consumer<Object> out) throws Throwable {
	}
}
