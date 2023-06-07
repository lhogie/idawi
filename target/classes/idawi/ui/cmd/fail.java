package idawi.ui.cmd;

import toools.io.file.RegularFile;

public class fail extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new fail(null).run(args);
	}

	public fail(RegularFile launcher) {
		super(launcher);
	}

	@Override
	public String getShortDescription() {
		return "simulates an error on the peers";
	}
}
