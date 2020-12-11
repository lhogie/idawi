package idawi.ui.cmd;

import toools.io.file.RegularFile;

public class pr extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new pr(null).run(args);
	}

	public pr(RegularFile launcher) {
		super(launcher);
	}

	@Override
	public String getShortDescription() {
		return "manipulates peer registry (list, add)";
	}
}
