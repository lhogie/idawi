package idawi.ui.cmd;

import idawi.Service;
import j4u.CommandLine;
import toools.io.Cout;
import toools.io.file.RegularFile;
import toools.thread.Threads;

public class deploy extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new deploy(null).run(args);
	}

	public deploy(RegularFile launcher) {
		super(launcher);
		addOption("--autonomous", "-a", null, null,
				"deployed peer will be autononous, i.e. they remain alive when parent dies");
		addOption("--rsync", null, null, null, "print the output of rsync");
		addOption("--to", "-s", ".+", null, "sets the hosts to deploy to");
	}

	@Override
	protected int work(Service localService, CommandLine cmdLine, double timeout)
			throws Throwable {
		int exitCode = super.work(localService, cmdLine, timeout);

		if (cmdLine.isOptionSpecified("--autonomous")) {
			Cout.info("Children are left running.");
		}
		else {
			Cout.info("Press Ctrl+C to quit.");
			Cout.info("Warning: children will lose connection and die.");
			Threads.sleepForever();
		}

		return exitCode;
	}

	@Override
	public String getShortDescription() {
		return "deploy";
	}
}
