package idawi;

import java.util.List;

public class OperationDescriptor implements Descriptor {
	public List<String> parameterTypes;
	public String name;
	public int nbCalls;
	public double totalDuration;
	public String impl;

	@Override
	public String toString() {
		return name + "(" + parameterTypes + ")";
	}
}
