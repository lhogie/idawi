package idawi.service;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.Message;
import idawi.MessageQueue.SUFFICIENCY;
import idawi.Service;
import idawi.ServiceAddress;

public class ExecService extends Service {
	public static interface Request extends Serializable {
		void execute(Consumer<Object> results) throws Throwable;
	}

	public ExecService(Component peer) {
		super(peer);
		registerOperation(null, in -> {
			var msg = in.get_blocking();
			((Request) msg.content).execute(r -> reply(msg, r));
		});
	}

	@Override
	public String getFriendlyName() {
		return "remote code executing";
	}

	public void exec(ServiceAddress to, double timeout, Request r, Function<Message, SUFFICIENCY> returns) {
		start(to, new OperationID(ExecService.class, null), true, r).returnQ.forEach(returns);
	}
}
