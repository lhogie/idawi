package idawi.ui.cmd;

import idawi.Component;
import j4u.CommandLine;
import j4u.CommandLineSpecification;
import toools.io.Cout;
import toools.reflect.Clazz;

public abstract class CommunicatingCommand extends IdawiCommand {

	static {
		Cout.timestamp();
	}

	@Override
	protected void specifyCmdLine(CommandLineSpecification spec) {
		spec.addOption("--timeout", "-t", ".*", 1, "timeout in second");
		spec.addOption("--repeat", "-r", "[0-9]+", "1", "repeats the command the given number of times");
	}

	public String getCommandName() {
		return Clazz.classNameWithoutPackage(getClass().getName());
	}

	@Override
	public int runScript(CommandLine cmdLine) throws Throwable {
		double timeout = Double.valueOf(getOptionValue(cmdLine, "--timeout"));

		Component localNode = new Component();

		int repeat = Integer.valueOf(cmdLine.getOptionValue("--repeat"));

		for (int i = 0; i < repeat; ++i) {

			if (repeat > 1) {
				Cout.debugSuperVisible("run #" + (i + 1));
			}

			int exitCode = work(localNode, cmdLine, timeout);

			if (exitCode != 0) {
				return exitCode;
			}
		}

		if (repeat > 1) {
			Cout.debugSuperVisible("completed");
		}

		return 0;
	}

	protected abstract int work(Component localNode, CommandLine cmdLine, double timeout) throws Throwable;
}
