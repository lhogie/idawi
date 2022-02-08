package idawi.service;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;

import idawi.Component;
import idawi.InnerOperation;
import idawi.Message;
import idawi.MessageQueue;
import idawi.MessageQueue.Enough;
import idawi.Service;
import idawi.To;

public class ExecService extends Service {
	public static interface Request extends Serializable {
		void execute(Consumer<Object> results) throws Throwable;
	}

	public ExecService(Component peer) {
		super(peer);
		registerOperation(new Exec());
	}

	@Override
	public String getFriendlyName() {
		return "remote code executing";
	}

	public class Exec extends InnerOperation {

		@Override
		public void exec(MessageQueue in) throws Throwable {
			var msg = in.get_blocking();
			((Request) msg.content).execute(r -> reply(msg, r));
		}

		@Override
		public String getDescription() {
			return "execute the given requests";
		}
	}

	public void exec(To to, double timeout, Request r, Function<Message, Enough> returns) {
		exec(to.s(ExecService.class).o("exec"), true, r).returnQ.forEach(returns);
	}
}
