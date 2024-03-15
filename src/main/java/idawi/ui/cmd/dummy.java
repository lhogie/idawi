package idawi.ui.cmd;

public class dummy extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new dummy().run(args);
	}

	@Override
	public String getDescription() {
		return "dummy command - do nothing, simply connects";
	}
}
