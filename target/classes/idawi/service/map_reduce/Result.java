package idawi.service.map_reduce;

import java.io.Serializable;

import idawi.Component;

public class Result<R> implements Serializable {
	public int taskID;
	public R value;
	public double receptionDate;
	public double completionDate;
	public Component worker;
}