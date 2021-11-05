package idawi;

import java.util.Set;
import java.util.TreeSet;

public class ServiceDescriptor implements Descriptor {
	public String name;
	public String description;
	public Set<OperationDescriptor> operationDescriptors = new TreeSet<>();
	public long nbMessagesReceived = 0;

	@Override
	public String toString() {
		String s = "service " + name;

		for (var o : operationDescriptors) {
			s += "- " + o + "\n";
		}

		s += nbMessagesReceived + " messages received";
		return s;
	}
}
