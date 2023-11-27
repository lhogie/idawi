package idawi.ui.cmd;

import j4u.CommandLineSpecification;

public class ls extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new ls().run(args);
	}

	@Override
	protected void specifyCmdLine(CommandLineSpecification spec) {
		spec.addOption("--list-queues", "-q", null, null, "list queues in every service");
	}

	@Override
	public String getDescription() {
		return "list services";
	}
}
