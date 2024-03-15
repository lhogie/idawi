package idawi.ui.cmd;

public class rt extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new rt().run(args);
	}

	@Override
	public String getDescription() {
		return "prints the content of the routing table";
	}
}
