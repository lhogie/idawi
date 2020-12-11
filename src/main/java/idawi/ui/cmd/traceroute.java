package idawi.ui.cmd;

import toools.io.file.RegularFile;

public class traceroute extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new traceroute(null).run(args);
	}

	public traceroute(RegularFile launcher) {
		super(launcher);
	}

	@Override
	public String getShortDescription() {
		return "prints the route to reach given node";
	}
}
