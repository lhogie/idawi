package idawi.ui.cmd;

import toools.io.file.RegularFile;

public class kill extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new kill(null).run(args);
	}

	public kill(RegularFile launcher) {
		super(launcher);
	}

	@Override
	public String getShortDescription() {
		return "kill";
	}
}
