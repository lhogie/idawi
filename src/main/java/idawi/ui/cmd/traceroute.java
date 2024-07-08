package idawi.ui.cmd;

public class traceroute extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new traceroute().run(args);
	}

	@Override
	public String getDescription() {
		return "prints the route to reach given node";
	}
}
