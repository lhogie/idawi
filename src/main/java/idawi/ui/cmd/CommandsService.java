package idawi.ui.cmd;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.messaging.Message;
import idawi.messaging.MessageQueue;

public class CommandsService extends Service {

	public CommandsService(Component peer) {
		super(peer);
	}


	public class exec extends InnerClassEndpoint {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			while (true) {
				Message m = in.poll_sync();

				if (m.isEOT()) {
					break;
				}

				((CommandBackend) m.content).runOnServer(component, r -> reply(m, r, false));
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