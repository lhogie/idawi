package idawi.ui.cmd;

public class kill extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new kill().run(args);
	}

	@Override
	public String getDescription() {
		return null;
	}
}
