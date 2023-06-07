package idawi.ui.cmd;

import toools.io.file.RegularFile;

public class ln extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new ln(null).run(args);
	}

	public ln(RegularFile launcher) {
		super(launcher);
		addOption("--protocols", "-p", null, null, "show protocols involved");
	}

	@Override
	public String getShortDescription() {
		return "list neighbors";
	}
}
