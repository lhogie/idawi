package idawi;

import idawi.messaging.MessageQueue;

public abstract class ProcedureNoInputEndpoint extends InnerClassEndpoint {

	@Override
	public final void impl(MessageQueue in) throws Throwable {
		doIt();
	}

	public abstract void doIt() throws Throwable;
}
