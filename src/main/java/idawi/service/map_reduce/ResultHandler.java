package idawi.service.map_reduce;

import idawi.messaging.Message;

public interface ResultHandler<R> {
	void newResult(Result<R> newResult);

	void newProgressMessage(String msg);

	void newProgressRatio(double r);

	void newMessage(Message a);
}