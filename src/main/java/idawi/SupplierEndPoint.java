package idawi;

import fr.cnrs.i3s.Cache;
import idawi.messaging.MessageQueue;

public abstract class SupplierEndPoint<O> extends InnerClassEndpoint<Void, O> {
	Cache<O> cache = new Cache<O>(cacheDuration(), null);

	@Override
	public final void impl(MessageQueue in) throws Throwable {
		O o = get();
		cache.set(o);
		reply(o, in.poll_sync());
	}

	protected double cacheDuration() {
		return 10;
	}


	public abstract O get() throws Throwable;
}
