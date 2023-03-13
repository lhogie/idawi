package idawi.service.map_reduce;

import java.io.Serializable;

import idawi.knowledge_base.ComponentRef;

public class Result<R> implements Serializable {
	public int taskID;
	public R value;
	public double receptionDate;
	public double completionDate;
	public ComponentRef worker;
}