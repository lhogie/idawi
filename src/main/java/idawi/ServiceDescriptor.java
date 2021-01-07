package idawi;

import java.util.HashSet;
import java.util.Set;

public class ServiceDescriptor implements Descriptor {
	public String name;
	public Set<OperationDescriptor> operationDescriptors = new HashSet<>();
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
