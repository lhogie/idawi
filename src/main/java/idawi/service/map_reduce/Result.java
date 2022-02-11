package idawi.service.map_reduce;

import java.io.Serializable;

import idawi.ComponentDescriptor;

public class Result<R> implements Serializable {
	public int taskID;
	public R value;
	public double receptionDate;
	public double completionDate;
	public ComponentDescriptor worker;
}