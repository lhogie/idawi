package idawi.ui.cmd;

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.IdawiOperation;
import idawi.Message;
import idawi.MessageQueue;
import idawi.Service;

public class CommandsService extends Service {

	public CommandsService(Component peer) {
		super(peer);
	}

	public static OperationID exec;

	@IdawiOperation
	public void exec(MessageQueue q) throws Throwable {
		while (true) {
			Message m = q.get_blocking();

			if (m.isEOT()) {
				break;
			}

			((CommandBackend) m.content).runOnServer(component, r -> reply(m, r));
		}
	}

	@Override
	public String getFriendlyName() {
		return "executes console command backends on nodes";
	}
}