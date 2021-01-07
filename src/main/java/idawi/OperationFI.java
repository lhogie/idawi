package idawi;

import java.util.function.Consumer;

public interface OperationFI {
	void accept(Message incomingMsg, Consumer<Object> returns) throws Throwable;
}
