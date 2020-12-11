package idawi.ui.cmd;

import toools.io.file.RegularFile;

public class rt extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new rt(null).run(args);
	}

	public rt(RegularFile launcher) {
		super(launcher);
	}

	@Override
	public String getShortDescription() {
		return "prints the content of the routing table";
	}
}
