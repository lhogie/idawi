package idawi.ui.cmd;

import toools.io.file.RegularFile;

public class dot extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new dot(null).run(args);
	}

	public dot(RegularFile launcher) {
		super(launcher);
	}

	@Override
	public String getShortDescription() {
		return "prints the DOT text of the map of the network";
	}
}
