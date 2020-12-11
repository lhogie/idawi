package idawi.ui.cmd;

import toools.io.file.RegularFile;

public class dummy extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new dummy(null).run(args);
	}

	public dummy(RegularFile launcher) {
		super(launcher);
	}

	@Override
	public String getShortDescription() {
		return "dummy command - do nothing, simply connects";
	}
}
