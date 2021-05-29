package idawi;

import java.util.List;

public class OperationDescriptor implements Descriptor, Comparable<OperationDescriptor> {
	public List<String> parameterTypes;
	public String name;
	public int nbCalls;
	public double totalDuration;
	public String impl;

	@Override
	public String toString() {
		return name + "(" + parameterTypes + ")";
	}

	@Override
	public int compareTo(OperationDescriptor o) {
		return name.compareTo(o.name);
	}
}
