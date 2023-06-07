package idawi.ui.cmd;

import toools.io.file.RegularFile;

public class bench extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new bench(null).run(args);
	}

	public bench(RegularFile launcher) {
		super(launcher);
		addOption("--size", "-s", "[0-9]+", 1000000, "size of the array");
	}

	@Override
	public String getShortDescription() {
		return "bench";
	}
}
