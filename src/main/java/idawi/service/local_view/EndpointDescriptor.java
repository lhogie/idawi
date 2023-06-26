package idawi.service.local_view;

import java.io.Serializable;

public class EndpointDescriptor implements Serializable, Comparable<EndpointDescriptor> {
	public String name, description;
	public int nbCalls;
	public double totalDuration;
	public String implementationClass;

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(EndpointDescriptor o) {
		return name.compareTo(o.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return ((EndpointDescriptor) o).name.equals(name);
	}
}
