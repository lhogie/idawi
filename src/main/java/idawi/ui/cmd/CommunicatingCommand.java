package idawi.ui.cmd;

import idawi.Component;
import idawi.knowledge_base.ComponentRef;
import j4u.CommandLine;
import j4u.License;
import toools.io.Cout;
import toools.io.file.RegularFile;
import toools.reflect.Clazz;

public abstract class CommunicatingCommand extends Command {

	static {
		Cout.timestamp();
	}

	public CommunicatingCommand(RegularFile launcher) {
		super(launcher);
		addOption("--timeout", "-t", ".*", 1, "timeout in second");
		addOption("--repeat", "-r", "[0-9]+", "1", "repeats the command the given number of times");

	}

	public String getCommandName() {
		return Clazz.classNameWithoutPackage(getClass().getName());
	}

	@Override
	public int runScript(CommandLine cmdLine) throws Throwable {
		double timeout = Double.valueOf(getOptionValue(cmdLine, "--timeout"));

		Component localNode = new Component(new ComponentRef(getCommandName()));

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

	protected abstract int work(Component c, CommandLine cmdLine, double timeout) throws Throwable;

	@Override
	public String getAuthor() {
		return "Luc Hogie";
	}

	@Override
	public License getLicence() {
		return License.ApacheLicenseV2;
	}

	@Override
	public String getApplicationName() {
		return "Idawi";
	}

	@Override
	public String getYear() {
		return "2019-2020";
	}
}
