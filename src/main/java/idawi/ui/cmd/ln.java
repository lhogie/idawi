package idawi.ui.cmd;

import j4u.CommandLineSpecification;

public class ln extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new ln().run(args);
	}

	@Override
	protected void specifyCmdLine(CommandLineSpecification spec) {
		spec.addOption("--protocols", "-p", null, null, "show protocols involved");
	}

	@Override
	public String getDescription() {
		return "list neighbors";
	}
}
