package idawi.ui.cmd;

import toools.io.file.RegularFile;

public class pdl extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new pdl(null).run(args);
	}

	public pdl(RegularFile launcher) {
		super(launcher);
	}

	@Override
	public String getShortDescription() {
		return "print PDL for given nodes";
	}
}
