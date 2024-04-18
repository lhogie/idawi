package idawi;

import idawi.messaging.MessageQueue;

public abstract class ProcedureEndpoint<I> extends InnerClassEndpoint<I, Void> {

	@Override
	public final void impl(MessageQueue in) throws Throwable {
		doIt(parms(in.poll_sync()));
	}

	public abstract void doIt(I in) throws Throwable;
}
