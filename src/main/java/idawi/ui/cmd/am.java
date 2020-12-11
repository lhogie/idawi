package idawi.ui.cmd;

import toools.io.file.RegularFile;

public class am extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new am(null).run(args);
	}

	public am(RegularFile launcher) {
		super(launcher);
	}

	@Override
	public String getShortDescription() {
		return "show active messages";
	}
}
