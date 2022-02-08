package idawi.ui.cmd;

import idawi.Component;
import idawi.InnerOperation;
import idawi.Message;
import idawi.MessageQueue;
import idawi.Service;

public class CommandsService extends Service {

	public CommandsService(Component peer) {
		super(peer);
		registerOperation(new exec());
	}

	public class exec extends InnerOperation {

		@Override
		public void exec(MessageQueue in) throws Throwable {
			while (true) {
				Message m = in.get_blocking();

				if (m.isEOT()) {
					break;
				}

				((CommandBackend) m.content).runOnServer(component, r -> reply(m, r));
			}
		}

		@Override
		public String getDescription() {
			return "exec commands";
		}

	}

	@Override
	public String getFriendlyName() {
		return "executes console command backends on nodes";
	}
}