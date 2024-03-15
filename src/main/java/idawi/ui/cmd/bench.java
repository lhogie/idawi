package idawi.ui.cmd;

import j4u.CommandLineSpecification;

public class bench extends BackendedCommand {

	@Override
	protected void specifyCmdLine(CommandLineSpecification spec) {
		spec.addOption("--size", "-s", "[0-9]+", 1000000, "size of the array");
	}

	@Override
	public String getDescription() {
		return "bench a component";
	}
}
