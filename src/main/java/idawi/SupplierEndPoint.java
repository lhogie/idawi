package idawi;

import idawi.messaging.MessageQueue;

public abstract class SupplierEndPoint<O> extends InnerClassEndpoint<Void, O> {

	@Override
	public final void impl(MessageQueue in) throws Throwable {
		reply(get(), in.poll_sync());
	}

	@Override
	public final String getDescription() {
		return "returns " + r();
	}

	protected abstract String r();

	public abstract O get() throws Throwable;
}
