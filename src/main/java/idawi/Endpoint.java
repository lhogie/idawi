package idawi;

import idawi.messaging.MessageQueue;

public interface Endpoint {
	void impl(MessageQueue in) throws Throwable;
}
