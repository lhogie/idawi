package idawi.ui.cmd;

import toools.io.file.RegularFile;

public class ls extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new ls(null).run(args);
	}

	public ls(RegularFile launcher) {
		super(launcher);
		addOption("--list-queues", "-q", null, null, "list queues in every service");
	}

	@Override
	public String getShortDescription() {
		return "list services";
	}
}
