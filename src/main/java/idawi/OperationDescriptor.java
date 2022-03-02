package idawi;

import java.util.ArrayList;
import java.util.List;

public class OperationDescriptor implements Descriptor, Comparable<OperationDescriptor> {
	public String name, description;
	public int nbCalls;
	public double totalDuration;
	public String implementationClass;

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(OperationDescriptor o) {
		return name.compareTo(o.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return ((OperationDescriptor) o).name.equals(name);
	}
}
