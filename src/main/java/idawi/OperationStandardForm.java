package idawi;

import java.util.function.Consumer;

public interface OperationStandardForm {
	void accept(Message incomingMsg, Consumer<Object> returns) throws Throwable;
}
