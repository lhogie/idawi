package idawi.ui.cmd;

public class fail extends BackendedCommand {
	public static void main(String[] args) throws Throwable {
		new fail().run(args);
	}

	@Override
	public String getDescription() {
		return "simulates an error on the peers";
	}
}
