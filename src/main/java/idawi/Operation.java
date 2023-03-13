package idawi;

import idawi.messaging.MessageQueue;

public interface Operation {
	void impl(MessageQueue in) throws Throwable;
}
