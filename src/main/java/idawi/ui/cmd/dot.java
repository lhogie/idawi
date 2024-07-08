package idawi.ui.cmd;

public class dot extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new dot().run(args);
	}

	@Override
	public String getDescription() {
		return "prints the DOT text of the map of the network";
	}
}
