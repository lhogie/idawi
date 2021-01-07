package idawi.service;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;

import idawi.Component;
import idawi.Message;
import idawi.MessageQueue.SUFFICIENCY;
import idawi.Service;
import idawi.To;

public class ExecService extends Service {
	public static interface Request extends Serializable {
		void execute(Consumer<Object> results) throws Throwable;
	}

	public ExecService(Component peer) {
		super(peer);
		registerOperation(null, (msg, results) -> ((Request) msg.content).execute(results));
	}

	@Override
	public String getFriendlyName() {
		return "remote code executing";
	}

	public void exec(To to, double timeout, Request r, Function<Message, SUFFICIENCY> returns) {
		send(r, to).forEach(returns);
	}

}
