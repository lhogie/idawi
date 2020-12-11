package idawi;

import java.io.Serializable;
import java.util.function.Consumer;

public abstract class AbstractOperation implements Serializable {
	int nbCalls;
	double totalDuration;

	public double avgDuration() {
		return totalDuration / nbCalls;
	}

	public abstract OperationDescriptor signature();

	protected final void accept(Message msg, Consumer<Object> returns) throws Throwable{
		++nbCalls;
		double start = Utils.time();
		acceptImpl(msg, returns);
		totalDuration += Utils.time() - start;
	}

	protected abstract void acceptImpl(Message msg, Consumer<Object> returns) throws Throwable;
}
