package idawi;

import java.util.function.Function;

import idawi.messaging.MessageQueue;

public abstract class FunctionEndPoint<I, O> extends InnerClassEndpoint<I, O> implements Function<I, O> {

	@Override
	public final void impl(MessageQueue in) throws Throwable {
		var msg = in.poll_sync();
		I parms = parms(msg);
		O response = f(parms);
		reply(response, msg);
	}

	@Override
	public O apply(I parms) {
		try {
			return f(parms);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	};

	public abstract O f(I parms) throws Throwable;
}
