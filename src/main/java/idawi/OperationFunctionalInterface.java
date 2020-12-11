package idawi;

import java.util.function.Consumer;

public interface OperationFunctionalInterface {
	void accept(Message incomingMsg, Consumer<Object> returns) throws Throwable;
}
