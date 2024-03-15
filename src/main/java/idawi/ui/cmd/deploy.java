package idawi.ui.cmd;

import idawi.Component;
import j4u.CommandLine;
import j4u.CommandLineSpecification;
import toools.io.Cout;
import toools.thread.Threads;

public class deploy extends BackendedCommand {

	@Override
	protected void specifyCmdLine(CommandLineSpecification spec) {
		spec.addOption("--autonomous", "-a", null, null,
				"deployed peer will be autononous, i.e. they remain alive when parent dies");
		spec.addOption("--rsync", null, null, null, "print the output of rsync");
		spec.addOption("--to", "-s", ".+", null, "sets the hosts to deploy to");
	}

	@Override
	protected int work(Component c, CommandLine cmdLine, double timeout) throws Throwable {
		int exitCode = super.work(c, cmdLine, timeout);

		if (cmdLine.isOptionSpecified("--autonomous")) {
			Cout.info("Children are left running.");
		} else {
			Cout.info("Press Ctrl+C to quit.");
			Cout.info("Warning: children will lose connection and die.");
			Threads.sleepForever();
		}

		return exitCode;
	}

	@Override
	public String getDescription() {
		return "deploy";
	}
}
