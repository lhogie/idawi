package idawi.service.map_reduce;

import java.io.Serializable;
import java.util.function.Consumer;

import idawi.To;

public abstract class Task<R> implements Serializable {
	// valid at a given round only
	int id;
	transient MapReduce mapReduceService;
	transient To to;

	public abstract R compute(Consumer output) throws Throwable;
}