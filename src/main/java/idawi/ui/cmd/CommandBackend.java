package idawi.ui.cmd;

import java.io.Serializable;
import java.util.function.Consumer;

import idawi.Component;
import j4u.CommandLine;

public abstract class CommandBackend implements Serializable {
	public CommandLine cmdline;

	abstract void runOnServer(Component thing, Consumer<Object> out)
			throws Throwable;
}