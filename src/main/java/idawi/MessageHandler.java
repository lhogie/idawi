package idawi;

import idawi.MessageQueue.Enough;

public interface MessageHandler {

	Enough newEOT(Message r);

	Enough newError(Message r);

	Enough newProgressMessage(Message r);

	Enough newProgressRatio(Message r);

	Enough newResult(Message r);

}
