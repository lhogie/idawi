package idawi.ui.cmd;

import idawi.Component;
import idawi.Service;

public class CommandsService extends Service {

	public CommandsService(Component peer) {
		super(peer);
		registerOperation(null, (msg, out) -> ((CommandBackend) msg.content).runOnServer(peer, out));
	}

	@Override
	public String getFriendlyName() {
		return "executes console command backends on nodes";
	}

}