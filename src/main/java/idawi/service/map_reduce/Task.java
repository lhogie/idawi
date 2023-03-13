package idawi.service.map_reduce;

import java.io.Serializable;
import java.util.function.Consumer;

import idawi.routing.MessageQDestination;
import idawi.routing.TargetComponents;

public abstract class Task<R> implements Serializable {
	// valid at a given round only
	int id;
	transient MapReduceService mapReduceService;
	transient TargetComponents to;

	public abstract R compute(Consumer output) throws Throwable;
}